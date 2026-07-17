package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib2.client.scene.SceneRenderContext;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.math.Ray;
import com.lowdragmc.lowdraglib2.math.Transform;
import com.lowdragmc.lowdraglib2.utils.Vector3fHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

/**
 * A Unity-style transform gizmo (move / rotate / scale) rendered on top of the scene.
 * <p>
 * The gizmo has no transform of its own that matters for rendering: it is drawn and picked entirely from
 * {@link #gizmoMatrix()} = {@code translate(targetPos) * rotate(orientation) * scale(screenConstant)}, so it
 * always sits on the target, faces the chosen {@link Space}, and keeps a constant on-screen size. Dragging is
 * resolved geometrically against the mouse ray (closest-point-on-axis for translate/scale, ray-vs-plane angle
 * for rotate) so the handle tracks the cursor exactly and rotation is independent of camera distance.
 */
public class TransformGizmo extends SceneObject implements ISceneRendering, ISceneInteractable {
    public static final RenderType POSITION_COLOR_NO_DEPTH = RenderType.create(
            "ldlib_position_color_no_depth",
            RenderSetup.builder(LDLibRenderPipelines.POSITION_COLOR_NO_DEPTH)
                    .sortOnUpload()
                    .createRenderSetup()
    );
    private static final RenderType NO_DEPTH_LINES = RenderType.create(
            "ldlib_no_depth_lines",
            RenderSetup.builder(LDLibRenderPipelines.NO_DEPTH_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );

    public enum Mode {
        NONE,
        TRANSLATE,
        ROTATE,
        SCALE
    }

    public enum Space {
        LOCAL,
        GLOBAL
    }

    /** Which handle a hover/drag is targeting. */
    private enum Handle {
        AXIS_X(0, false), AXIS_Y(1, false), AXIS_Z(2, false),
        PLANE_X(0, true), PLANE_Y(1, true), PLANE_Z(2, true);
        final int axis;      // 0=X, 1=Y, 2=Z
        final boolean plane; // true = planar handle, false = axis handle
        Handle(int axis, boolean plane) { this.axis = axis; this.plane = plane; }
    }

    // gizmo geometry, authored in gizmo-units (a constant screen scale is applied on top)
    private static final float BASE_SCALE = 0.23f;
    private static final float AXIS_LENGTH = 1.0f;
    private static final float SHAFT_RADIUS = 0.02f;
    private static final float ARROW_RADIUS = 0.07f;
    private static final float ARROW_HEIGHT = 0.22f;
    private static final float PLANE_MIN = 0.12f;
    private static final float PLANE_MAX = 0.32f;
    private static final float SCALE_SHAFT_LENGTH = 0.9f;
    private static final float SCALE_BOX_HALF = 0.08f;
    private static final float RING_RADIUS = 1.0f;
    private static final int RING_SEGMENTS = 64;
    private static final int ARC_SEGMENTS = 48;
    private static final int SHAFT_SEGMENTS = 12;
    /** Screen-space pixel width for the ring/guide lines (matches 1.21's LineStateShard(3f)). */
    private static final float LINE_WIDTH = 3f;

    // snapping increments (Ctrl held)
    private static final float SNAP_TRANSLATE = 0.25f;
    private static final float SNAP_SCALE = 0.25f;
    private static final float SNAP_ROTATE = (float) Math.toRadians(15);

    private static final VoxelShape xAxisCollider = Shapes.box(0, -0.1, -0.1, 1.2, 0.1, 0.1);
    private static final VoxelShape xPlaneCollider = Shapes.box(0, 0.1, 0.1, 0.01, 0.3, 0.3);
    private static final VoxelShape yAxisCollider = Shapes.box(-0.1, 0, -0.1, 0.1, 1.2, 0.1);
    private static final VoxelShape yPlaneCollider = Shapes.box(0.1, 0, 0.1, 0.3, 0.01, 0.3);
    private static final VoxelShape zAxisCollider = Shapes.box(-0.1, -0.1, 0, 0.1, 0.1, 1.2);
    private static final VoxelShape zPlaneCollider = Shapes.box(0.1, 0.1, 0, 0.3, 0.3, 0.01);
    private static final VoxelShape xRingCollider = createRingCollisionBox(
            new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), 1.0, 16, 0.1);
    private static final VoxelShape yRingCollider = createRingCollisionBox(
            new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), 1.0, 16, 0.1);
    private static final VoxelShape zRingCollider = createRingCollisionBox(
            new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), 1.0, 16, 0.1);

    @Nullable
    @Getter
    private Transform targetTransform;
    @Nullable
    @Setter
    private Runnable onTransformChanged;

    @Getter
    @Nonnull
    private Mode mode = Mode.NONE;
    @Getter
    @Nonnull
    private Space space = Space.LOCAL;
    @Getter
    private boolean enabled = true;

    // runtime
    @Nullable
    private Handle hoverHandle;
    @Nullable
    private Handle dragHandle;
    // drag state, all captured at drag start (world space)
    private Vector3f dragAxis;          // unit world axis / plane normal
    private Vector3f dragStartPosition; // target world position at grab
    private Quaternionf dragStartRotation;
    private Vector3f dragStartScale;    // target local scale at grab
    private Vector3f dragGrabOffset;    // plane drag: grabbed hit - center
    private float dragStartParam;       // axis translate/scale: along-axis distance at grab
    private Vector3f dragStartHandleDir; // rotate: unit grabbed direction from center (world)
    private float dragRotateAccum;      // rotate: continuous accumulated angle (unwrapped, unsnapped)
    private float dragPrevRaw;          // rotate: previous frame's raw signed angle (for unwrapping)
    private float dragRotateAngle;      // rotate: applied/displayed angle (possibly snapped)
    @Nullable
    @Getter
    private String readoutText;         // transient HUD text shown while dragging

    // ---------------------------------------------------------------------------------------------
    // state / lifecycle
    // ---------------------------------------------------------------------------------------------

    public void setTargetTransform(@Nullable Transform targetTransform) {
        if (this.targetTransform == targetTransform) return;
        endDrag();
        this.targetTransform = targetTransform;
    }

    public void setMode(@Nonnull Mode mode) {
        if (this.mode == mode) return;
        this.mode = mode;
        endDrag();
    }

    public void setSpace(@Nonnull Space space) {
        if (this.space == space) return;
        this.space = space;
        endDrag();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!isActive()) endDrag();
    }

    public boolean hasTargetTransform() {
        return targetTransform != null;
    }

    /** The single source of truth for whether the gizmo should render and accept interaction. */
    public boolean isActive() {
        return enabled && targetTransform != null && mode != Mode.NONE;
    }

    public boolean isDragging() {
        return dragHandle != null;
    }

    /** The gizmo orientation in world space. Scale is always local; translate/rotate honour {@link Space}. */
    private Quaternionf orientation() {
        if (targetTransform == null) return new Quaternionf();
        if (mode == Mode.SCALE || space == Space.LOCAL) {
            return targetTransform.rotation();
        }
        return new Quaternionf();
    }

    private float computeGizmoScale() {
        if (targetTransform == null || !(getScene() instanceof SceneEditor editor)) return 1f;
        var renderer = editor.scene.<WorldSceneRenderer>getRenderer();
        if (renderer == null) return 1f;
        var distance = renderer.getEyePos().distance(targetTransform.position());
        return distance * (float) Math.tan(renderer.getFov() * 0.5f * Math.PI / 180) * BASE_SCALE;
    }

    /** translate(targetPos) * rotate(orientation) * scale(screenConstant); shared by render and picking. */
    private Matrix4f gizmoMatrix() {
        if (targetTransform == null) return new Matrix4f();
        return new Matrix4f()
                .translate(targetTransform.position())
                .rotate(orientation())
                .scale(computeGizmoScale());
    }

    private Vector3f localAxis(int idx) {
        return switch (idx) {
            case 0 -> new Vector3f(1, 0, 0);
            case 1 -> new Vector3f(0, 1, 0);
            default -> new Vector3f(0, 0, 1);
        };
    }

    private Vector3f worldAxis(int idx) {
        return orientation().transform(localAxis(idx)).normalize();
    }

    // ---------------------------------------------------------------------------------------------
    // picking
    // ---------------------------------------------------------------------------------------------

    /** Transform a world-space ray into gizmo-local space (where the colliders live). */
    private Ray toGizmoSpace(Ray ray) {
        return ray.transform(gizmoMatrix().invert()).toInfinite();
    }

    private void updateHover() {
        hoverHandle = null;
        if (!isActive() || !(getScene() instanceof SceneEditor editor)) return;
        var worldRay = editor.getMouseRay().orElse(null);
        if (worldRay == null) return;
        var ray = toGizmoSpace(worldRay);
        if (mode == Mode.TRANSLATE) {
            if (ray.clip(xPlaneCollider) != null) { hoverHandle = Handle.PLANE_X; return; }
            if (ray.clip(yPlaneCollider) != null) { hoverHandle = Handle.PLANE_Y; return; }
            if (ray.clip(zPlaneCollider) != null) { hoverHandle = Handle.PLANE_Z; return; }
        }
        VoxelShape xc, yc, zc;
        if (mode == Mode.ROTATE) {
            xc = xRingCollider; yc = yRingCollider; zc = zRingCollider;
        } else { // TRANSLATE or SCALE
            xc = xAxisCollider; yc = yAxisCollider; zc = zAxisCollider;
        }
        if (ray.clip(xc) != null) hoverHandle = Handle.AXIS_X;
        else if (ray.clip(yc) != null) hoverHandle = Handle.AXIS_Y;
        else if (ray.clip(zc) != null) hoverHandle = Handle.AXIS_Z;
    }

    @Override
    public boolean onMouseClick(Ray mouseRay) {
        if (!isActive()) return false;
        updateHover();
        if (hoverHandle == null) return false;
        dragHandle = hoverHandle;
        beginDrag(mouseRay);
        return true;
    }

    private void beginDrag(Ray worldRay) {
        assert targetTransform != null && dragHandle != null;
        var center = targetTransform.position();
        dragStartPosition = new Vector3f(center);
        dragStartRotation = targetTransform.rotation();
        dragStartScale = new Vector3f(targetTransform.localScale());
        dragAxis = worldAxis(dragHandle.axis);
        dragRotateAccum = 0;
        dragPrevRaw = 0;
        dragRotateAngle = 0;
        readoutText = null;

        var origin = worldRay.startPos();
        var dir = worldRay.getDirection();
        if (dragHandle.plane) {
            var hit = rayPlaneIntersect(origin, dir, center, dragAxis);
            dragGrabOffset = hit == null ? new Vector3f() : new Vector3f(hit).sub(center);
        } else if (mode == Mode.ROTATE) {
            var hit = rayPlaneIntersect(origin, dir, center, dragAxis);
            var handleDir = hit == null ? new Vector3f() : new Vector3f(hit).sub(center);
            if (handleDir.lengthSquared() < 1.0e-9f) {
                // fallback: any vector perpendicular to the axis
                handleDir = perpendicular(dragAxis);
            }
            dragStartHandleDir = handleDir.normalize();
        } else { // axis translate / scale
            var closest = Vector3fHelper.closestPointOnLine(origin, dir, center, dragAxis);
            dragStartParam = new Vector3f(closest).sub(center).dot(dragAxis);
        }
    }

    @Override
    public void onMouseRelease(Ray mouseRay) {
        endDrag();
    }

    private void endDrag() {
        dragHandle = null;
        dragAxis = null;
        dragStartPosition = null;
        dragStartRotation = null;
        dragStartScale = null;
        dragGrabOffset = null;
        dragStartHandleDir = null;
        dragStartParam = 0;
        dragRotateAccum = 0;
        dragPrevRaw = 0;
        dragRotateAngle = 0;
        readoutText = null;
    }

    // ---------------------------------------------------------------------------------------------
    // drag resolution (runs per frame)
    // ---------------------------------------------------------------------------------------------

    @Override
    public void updateFrame(float partialTicks) {
        super.updateFrame(partialTicks);
        if (targetTransform == null || !(getScene() instanceof SceneEditor editor)) {
            endDrag();
            return;
        }
        if (dragHandle != null) {
            editor.getMouseRay().ifPresent(this::applyDrag);
        } else {
            updateHover();
        }
    }

    private void applyDrag(Ray worldRay) {
        assert targetTransform != null && dragHandle != null;
        var origin = worldRay.startPos();
        var dir = worldRay.getDirection();
        var snap = UIElement.isControlDown();
        var changed = false;

        if (dragHandle.plane) {
            var hit = rayPlaneIntersect(origin, dir, dragStartPosition, dragAxis);
            if (hit != null) {
                var newPos = new Vector3f(hit).sub(dragGrabOffset);
                if (snap) snapVector(newPos, SNAP_TRANSLATE);
                targetTransform.position(newPos);
                readoutText = fmt(new Vector3f(newPos).sub(dragStartPosition));
                changed = true;
            }
        } else if (mode == Mode.ROTATE) {
            var hit = rayPlaneIntersect(origin, dir, dragStartPosition, dragAxis);
            if (hit != null) {
                var currentDir = new Vector3f(hit).sub(dragStartPosition);
                if (currentDir.lengthSquared() > 1.0e-9f) {
                    currentDir.normalize();
                    // signedAngle wraps at ±180°; unwrap the per-frame delta so rotations can exceed a half turn
                    var raw = Vector3fHelper.signedAngle(dragStartHandleDir, currentDir, dragAxis);
                    double d = raw - dragPrevRaw;
                    if (d > Math.PI) d -= 2 * Math.PI;
                    else if (d < -Math.PI) d += 2 * Math.PI;
                    dragPrevRaw = raw;
                    dragRotateAccum += (float) d;
                    var angle = snap ? Math.round(dragRotateAccum / SNAP_ROTATE) * SNAP_ROTATE : dragRotateAccum;
                    dragRotateAngle = angle;
                    var delta = new Quaternionf().fromAxisAngleRad(dragAxis.x, dragAxis.y, dragAxis.z, angle);
                    targetTransform.rotation(delta.mul(dragStartRotation, new Quaternionf()));
                    readoutText = "%.1f°".formatted(Math.toDegrees(angle));
                    changed = true;
                }
            }
        } else if (mode == Mode.TRANSLATE) {
            var closest = Vector3fHelper.closestPointOnLine(origin, dir, dragStartPosition, dragAxis);
            var delta = new Vector3f(closest).sub(dragStartPosition).dot(dragAxis) - dragStartParam;
            if (snap) delta = Math.round(delta / SNAP_TRANSLATE) * SNAP_TRANSLATE;
            var newPos = new Vector3f(dragStartPosition).add(new Vector3f(dragAxis).mul(delta));
            targetTransform.position(newPos);
            readoutText = fmt(new Vector3f(dragAxis).mul(delta));
            changed = true;
        } else if (mode == Mode.SCALE) {
            var closest = Vector3fHelper.closestPointOnLine(origin, dir, dragStartPosition, dragAxis);
            var worldDelta = new Vector3f(closest).sub(dragStartPosition).dot(dragAxis) - dragStartParam;
            var gizmoScale = computeGizmoScale();
            var delta = gizmoScale > 1.0e-6f ? worldDelta / gizmoScale : worldDelta; // measure in gizmo-units
            var idx = dragHandle.axis;
            var newComp = dragStartScale.get(idx) + delta;
            if (snap) newComp = Math.round(newComp / SNAP_SCALE) * SNAP_SCALE;
            var newScale = new Vector3f(dragStartScale).setComponent(idx, newComp);
            targetTransform.localScale(newScale);
            readoutText = fmt(newScale);
            changed = true;
        }

        if (changed && onTransformChanged != null) {
            onTransformChanged.run();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // rendering
    // ---------------------------------------------------------------------------------------------

    @Override
    public void preDraw(float partialTicks) {
    }

    @Override
    public void postDraw(float partialTicks) {
    }

    @Override
    public void draw(SceneRenderContext ctx) {
        if (targetTransform == null) return;
        var poseStack = ctx.poseStack();
        poseStack.pushPose();
        poseStack.mulPose(gizmoMatrix());
        drawInternal(ctx);
        poseStack.popPose();
    }

    @Override
    public void drawInternal(SceneRenderContext ctx) {
        if (targetTransform == null) return;
        var poseStack = ctx.poseStack();
        // 26.2: no MultiBufferSource — batch per-RenderType immediate draws and flush at the end.
        try (var immediate = new RenderUtils.ImmediateDraw()) {
            switch (mode) {
                case TRANSLATE -> drawTranslate(poseStack, immediate);
                case SCALE -> drawScale(poseStack, immediate);
                case ROTATE -> drawRotate(poseStack, immediate);
                default -> { }
            }
        }
    }

    private void drawTranslate(PoseStack poseStack, RenderUtils.ImmediateDraw bufferSource) {
        // guide line for the active axis while dragging
        if (dragHandle != null && !dragHandle.plane) {
            var line = bufferSource.getBuffer(NO_DEPTH_LINES);
            drawInfiniteAxisLine(poseStack, line, dragHandle.axis);
        }
        var buffer = bufferSource.getBuffer(POSITION_COLOR_NO_DEPTH);
        for (int idx = 0; idx < 3; idx++) {
            if (!isAxisVisible(idx)) continue;
            var c = axisColor(idx, isAxisHighlighted(idx));
            var axis = axisEnum(idx);
            RenderBufferUtils.shapeCylinder(poseStack, buffer, 0, 0, 0, SHAFT_RADIUS, AXIS_LENGTH, SHAFT_SEGMENTS,
                    c[0], c[1], c[2], c[3], axis);
            var tip = axisPoint(idx, AXIS_LENGTH);
            RenderBufferUtils.shapeCone(poseStack, buffer, tip.x, tip.y, tip.z, ARROW_RADIUS, ARROW_HEIGHT, 12,
                    c[0], c[1], c[2], c[3], axis);
            RenderBufferUtils.shapeCircle(poseStack, buffer, tip.x, tip.y, tip.z, ARROW_RADIUS, 12,
                    c[0], c[1], c[2], c[3], axis);
        }
        // planar handles
        for (int idx = 0; idx < 3; idx++) {
            if (!isPlaneVisible(idx)) continue;
            var c = axisColor(idx, isPlaneHighlighted(idx));
            drawPlaneQuad(poseStack, buffer, idx, c);
        }
    }

    private void drawScale(PoseStack poseStack, RenderUtils.ImmediateDraw bufferSource) {
        if (dragHandle != null && !dragHandle.plane) {
            var line = bufferSource.getBuffer(NO_DEPTH_LINES);
            drawInfiniteAxisLine(poseStack, line, dragHandle.axis);
        }
        var buffer = bufferSource.getBuffer(POSITION_COLOR_NO_DEPTH);
        for (int idx = 0; idx < 3; idx++) {
            if (!isAxisVisible(idx)) continue;
            var c = axisColor(idx, isAxisHighlighted(idx));
            var axis = axisEnum(idx);
            RenderBufferUtils.shapeCylinder(poseStack, buffer, 0, 0, 0, SHAFT_RADIUS, SCALE_SHAFT_LENGTH, SHAFT_SEGMENTS,
                    c[0], c[1], c[2], c[3], axis);
            var end = axisPoint(idx, AXIS_LENGTH);
            RenderBufferUtils.drawCubeFace(poseStack, buffer,
                    end.x - SCALE_BOX_HALF, end.y - SCALE_BOX_HALF, end.z - SCALE_BOX_HALF,
                    end.x + SCALE_BOX_HALF, end.y + SCALE_BOX_HALF, end.z + SCALE_BOX_HALF,
                    c[0], c[1], c[2], c[3], true);
        }
    }

    private void drawRotate(PoseStack poseStack, RenderUtils.ImmediateDraw bufferSource) {
        var line = bufferSource.getBuffer(NO_DEPTH_LINES);
        for (int idx = 0; idx < 3; idx++) {
            if (!isAxisVisible(idx)) continue;
            var c = axisColor(idx, isAxisHighlighted(idx));
            RenderBufferUtils.drawCircleLine(poseStack, line, new Vector3f(0, 0, 0), localAxis(idx), RING_SEGMENTS,
                    RING_RADIUS, c[0], c[1], c[2], c[3], LINE_WIDTH);
        }
        // world-space angle indicator while rotating
        if (dragHandle != null && dragStartHandleDir != null) {
            drawRotateIndicator(poseStack, bufferSource);
        }
    }

    /** Draws the translucent swept-angle sector + guide lines in world space (undoing the gizmo matrix). */
    private void drawRotateIndicator(PoseStack poseStack, RenderUtils.ImmediateDraw bufferSource) {
        if (targetTransform == null) return;
        poseStack.pushPose();
        poseStack.mulPose(gizmoMatrix().invert()); // back to world space
        var center = targetTransform.position();
        var radius = RING_RADIUS * computeGizmoScale();
        var u = new Vector3f(dragStartHandleDir);
        var v = new Vector3f(dragAxis).cross(u, new Vector3f());
        if (v.lengthSquared() > 1.0e-9f) v.normalize();

        var tri = bufferSource.getBuffer(POSITION_COLOR_NO_DEPTH);
        RenderBufferUtils.shapeSector(poseStack, tri, center, u, v, radius, 0, dragRotateAngle, ARC_SEGMENTS,
                1f, 1f, 0f, 0.35f);

        var line = bufferSource.getBuffer(NO_DEPTH_LINES);
        var startPt = new Vector3f(center).add(new Vector3f(u).mul(radius));
        var endDir = new Vector3f(u).mul(Mth.cos(dragRotateAngle)).add(new Vector3f(v).mul(Mth.sin(dragRotateAngle)));
        var endPt = new Vector3f(center).add(endDir.mul(radius));
        RenderBufferUtils.drawLine(poseStack.last(), line, center, startPt, 1f, 1f, 1f, 0.5f, 1f, 1f, 1f, 0.5f, LINE_WIDTH, LINE_WIDTH);
        RenderBufferUtils.drawLine(poseStack.last(), line, center, endPt, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, LINE_WIDTH, LINE_WIDTH);
        poseStack.popPose();
    }

    private void drawInfiniteAxisLine(PoseStack poseStack, VertexConsumer buffer, int idx) {
        var far = axisPoint(idx, 50f);
        RenderBufferUtils.drawLine(poseStack.last(), buffer, new Vector3f(far).negate(), far,
                1f, 1f, 1f, 0.6f, 1f, 1f, 1f, 0.6f, LINE_WIDTH, LINE_WIDTH);
    }

    private void drawPlaneQuad(PoseStack poseStack, VertexConsumer buffer, int idx, float[] c) {
        switch (idx) {
            case 0 -> RenderBufferUtils.drawCubeFace(poseStack, buffer, 0, PLANE_MIN, PLANE_MIN, 0, PLANE_MAX, PLANE_MAX,
                    c[0], c[1], c[2], c[3] * 0.6f, false);
            case 1 -> RenderBufferUtils.drawCubeFace(poseStack, buffer, PLANE_MIN, 0, PLANE_MIN, PLANE_MAX, 0, PLANE_MAX,
                    c[0], c[1], c[2], c[3] * 0.6f, false);
            default -> RenderBufferUtils.drawCubeFace(poseStack, buffer, PLANE_MIN, PLANE_MIN, 0, PLANE_MAX, PLANE_MAX, 0,
                    c[0], c[1], c[2], c[3] * 0.6f, false);
        }
    }

    private Direction.Axis axisEnum(int idx) {
        return switch (idx) {
            case 0 -> Direction.Axis.X;
            case 1 -> Direction.Axis.Y;
            default -> Direction.Axis.Z;
        };
    }

    private Vector3f axisPoint(int idx, float len) {
        return switch (idx) {
            case 0 -> new Vector3f(len, 0, 0);
            case 1 -> new Vector3f(0, len, 0);
            default -> new Vector3f(0, 0, len);
        };
    }

    /** @return {r, g, b, a}; highlighted handles are yellow, otherwise the per-axis colour. */
    private float[] axisColor(int idx, boolean highlight) {
        if (highlight) return new float[]{1f, 1f, 0f, 1f};
        return switch (idx) {
            case 0 -> new float[]{1f, 0f, 0f, 1f};
            case 1 -> new float[]{0f, 1f, 0f, 1f};
            default -> new float[]{0f, 0f, 1f, 1f};
        };
    }

    private Handle activeHandle() {
        return dragHandle != null ? dragHandle : hoverHandle;
    }

    private boolean isAxisHighlighted(int idx) {
        var h = activeHandle();
        return h != null && !h.plane && h.axis == idx;
    }

    private boolean isPlaneHighlighted(int idx) {
        var h = activeHandle();
        return h != null && h.plane && h.axis == idx;
    }

    private boolean isAxisVisible(int idx) {
        return dragHandle == null || (!dragHandle.plane && dragHandle.axis == idx);
    }

    private boolean isPlaneVisible(int idx) {
        return dragHandle == null || (dragHandle.plane && dragHandle.axis == idx);
    }

    // ---------------------------------------------------------------------------------------------
    // math helpers
    // ---------------------------------------------------------------------------------------------

    @Nullable
    private static Vector3f rayPlaneIntersect(Vector3f origin, Vector3f dir, Vector3f planePoint, Vector3f planeNormal) {
        var denom = dir.dot(planeNormal);
        if (Math.abs(denom) < 1.0e-6f) return null;
        var t = new Vector3f(planePoint).sub(origin).dot(planeNormal) / denom;
        return new Vector3f(origin).add(new Vector3f(dir).mul(t));
    }

    private static Vector3f perpendicular(Vector3f v) {
        var ref = Math.abs(v.x) < 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        return ref.cross(v).normalize();
    }

    private static void snapVector(Vector3f v, float step) {
        v.set(Math.round(v.x / step) * step, Math.round(v.y / step) * step, Math.round(v.z / step) * step);
    }

    private static String fmt(Vector3f v) {
        return "%.2f, %.2f, %.2f".formatted(v.x, v.y, v.z);
    }

    public static VoxelShape createRingCollisionBox(Vector3f center, Vector3f normal, double radius, int segments, double thickness) {
        VoxelShape ringShape = Shapes.empty();
        double angleStep = 2 * Math.PI / segments;

        Vector3f u = new Vector3f();
        Vector3f v = new Vector3f();

        if (normal.equals(new Vector3f(0, 0, 1)) || normal.equals(new Vector3f(0, 0, -1))) {
            u.set(1, 0, 0);
            v.set(0, 1, 0);
        } else {
            if (Math.abs(normal.x) < Math.abs(normal.y) && Math.abs(normal.x) < Math.abs(normal.z)) {
                u.set(0, -normal.z, normal.y).normalize();
            } else if (Math.abs(normal.y) < Math.abs(normal.x) && Math.abs(normal.y) < Math.abs(normal.z)) {
                u.set(-normal.z, 0, normal.x).normalize();
            } else {
                u.set(-normal.y, normal.x, 0).normalize();
            }
            v.set(normal).cross(u).normalize();
            u.cross(normal, v).normalize();
        }

        for (int i = 0; i < segments; i++) {
            double angle = i * angleStep;
            double nextAngle = (i + 1) * angleStep;

            Vector3f start = new Vector3f(center)
                    .add((float) (radius * Math.cos(angle) * u.x + radius * Math.sin(angle) * v.x),
                            (float) (radius * Math.cos(angle) * u.y + radius * Math.sin(angle) * v.y),
                            (float) (radius * Math.cos(angle) * u.z + radius * Math.sin(angle) * v.z));

            Vector3f end = new Vector3f(center)
                    .add((float) (radius * Math.cos(nextAngle) * u.x + radius * Math.sin(nextAngle) * v.x),
                            (float) (radius * Math.cos(nextAngle) * u.y + radius * Math.sin(nextAngle) * v.y),
                            (float) (radius * Math.cos(nextAngle) * u.z + radius * Math.sin(nextAngle) * v.z));

            double minX = Math.min(start.x, end.x) - thickness / 2;
            double maxX = Math.max(start.x, end.x) + thickness / 2;
            double minY = Math.min(start.y, end.y) - thickness / 2;
            double maxY = Math.max(start.y, end.y) + thickness / 2;
            double minZ = Math.min(start.z, end.z) - thickness / 2;
            double maxZ = Math.max(start.z, end.z) + thickness / 2;

            VoxelShape segmentBox = Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            ringShape = Shapes.or(ringShape, segmentBox);
        }

        return ringShape;
    }
}
