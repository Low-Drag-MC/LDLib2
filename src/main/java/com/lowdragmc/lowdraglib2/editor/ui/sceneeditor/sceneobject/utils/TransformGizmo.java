package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneInteractable;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.math.Ray;
import com.lowdragmc.lowdraglib2.math.ITransform;
import com.lowdragmc.lowdraglib2.math.Transform;
import com.lowdragmc.lowdraglib2.utils.Vector3fHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.AxisAngle4f;
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
 *
 * <h2>Rotating</h2>
 * {@link Mode#ROTATE} follows Unreal's widget and offers four handles, in this pick order:
 * <ul>
 *   <li>the three coloured {@linkplain Handle#AXIS_X axis rings}, which constrain the turn to one axis;</li>
 *   <li>the outer {@linkplain Handle#SCREEN screen ring}, which turns the target in the plane of the screen —
 *       about the eye-to-gizmo direction, frozen when the drag starts;</li>
 *   <li>the {@linkplain Handle#TRACKBALL ball} filling the rest of the gizmo, where a drag rolls the target
 *       freely with the grabbed point following the cursor.</li>
 * </ul>
 * The rings are drawn as tubes rather than lines and picked analytically against an explicit tolerance that is
 * wider than they are drawn, because a line three pixels across is not something anyone can aim at.
 */
@OnlyIn(Dist.CLIENT)
public class TransformGizmo extends SceneObject implements ISceneRendering, ISceneInteractable {
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

    /**
     * Which handle a hover/drag is targeting.
     *
     * <p>{@link #SCREEN} and {@link #TRACKBALL} belong to {@link Mode#ROTATE} only and have no axis of
     * their own — both turn about the camera, so {@link #axis} is {@code -1} for them and
     * {@link #isAxisAligned()} is how to ask rather than comparing the number.
     */
    public enum Handle {
        AXIS_X(0, false), AXIS_Y(1, false), AXIS_Z(2, false),
        PLANE_X(0, true), PLANE_Y(1, true), PLANE_Z(2, true),
        /** The outer ring: rotate in the plane of the screen, about the camera's view direction. */
        SCREEN(-1, false),
        /** Anywhere inside the ball: free rotation, the grabbed point following the cursor. */
        TRACKBALL(-1, false);

        public final int axis;      // 0=X, 1=Y, 2=Z, -1 = camera-relative
        public final boolean plane; // true = planar handle, false = axis handle

        Handle(int axis, boolean plane) { this.axis = axis; this.plane = plane; }

        /** Whether this handle is tied to one of the gizmo's three axes rather than to the camera. */
        public boolean isAxisAligned() { return axis >= 0; }
    }

    /** The axis handles by axis index, so a loop over X, Y, Z does not need a switch to name its answer. */
    private static final Handle[] AXIS_HANDLES = {Handle.AXIS_X, Handle.AXIS_Y, Handle.AXIS_Z};

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
    /** Radius of the three axis rotation rings. */
    public static final float RING_RADIUS = 1.0f;
    /**
     * Radius of the outer, view-facing {@link Handle#SCREEN} ring. Far enough outside the axis rings that
     * their projections cannot reach it, so aiming at it is never ambiguous.
     */
    public static final float SCREEN_RING_RADIUS = 1.28f;
    /** Radius of the {@link Handle#TRACKBALL} ball; matches the axis rings, as it does in UE. */
    public static final float TRACKBALL_RADIUS = RING_RADIUS;
    /**
     * Half the drawn thickness of a rotation ring. Rings are tubes rather than lines because a line's
     * width is one shared setting on the render type, which is both too thin here and not ours to change.
     */
    private static final float RING_TUBE_RADIUS = 0.032f;
    /**
     * How far off a ring the cursor may be and still grab it. Several times wider than the ring is drawn,
     * on purpose: a handle you have to hit exactly is what made rotating unpleasant, and the fix for that
     * is a forgiving hit area rather than a fat ring covering the model.
     */
    private static final float RING_PICK_TOLERANCE = 0.09f;
    private static final int RING_SEGMENTS = 64;
    private static final int RING_TUBE_SEGMENTS = 6;
    private static final int ARC_SEGMENTS = 48;
    private static final int SHAFT_SEGMENTS = 12;

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

    /**
     * What is being dragged.
     *
     * <p>{@link ITransform} rather than {@link Transform}: a gizmo needs six operations — world
     * position and rotation, local scale, each read and written — and demanding a whole scene-object
     * transform for those forced anything with its own transform to keep a shadow one beside it and
     * reconcile the two every frame.
     */
    @Nullable
    private ITransform targetTransform;
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
    @Getter
    private Handle hoverHandle;
    @Nullable
    @Getter
    private Handle dragHandle;
    // drag state, all captured at drag start (world space)
    private Vector3f dragAxis;          // unit world axis / plane normal
    private Vector3f dragStartPosition; // target world position at grab
    private Quaternionf dragStartRotation;
    private Vector3f dragStartScale;    // target local scale at grab
    private Vector3f dragGrabOffset;    // plane drag: grabbed hit - center
    private float dragStartParam;       // axis translate/scale: along-axis distance at grab
    @Nullable
    private Vector3f dragStartHandleDir; // ring rotate: unit grabbed direction from center (world)
    private float dragRotateAccum;      // rotate: continuous accumulated angle (unwrapped, unsnapped)
    private float dragPrevRaw;          // rotate: previous frame's raw signed angle (for unwrapping)
    private float dragRotateAngle;      // rotate: applied/displayed angle (possibly snapped)
    @Nullable
    private Vector3f dragBallStart;     // trackball: unit grabbed point on the ball (world)
    private float dragBallRadius;       // trackball: ball radius at grab, so the mapping cannot shift mid-drag
    @Nullable
    @Getter
    private String readoutText;         // transient HUD text shown while dragging

    // ---------------------------------------------------------------------------------------------
    // state / lifecycle
    // ---------------------------------------------------------------------------------------------

    /**
     * ⚠️ <b>The one signature this change widened.</b> This returned {@link Transform} until 2.2.38
     * and now returns {@link ITransform}, which is a binary break for anything that called it — there
     * is no compatible alternative, because a target that is not a {@code Transform} cannot be
     * returned as one, and answering {@code null} for it would be a lie rather than a limitation.
     * Every other signature involved kept a {@code Transform} overload.
     */
    @Nullable
    public ITransform getTargetTransform() {
        return targetTransform;
    }

    public void setTargetTransform(@Nullable ITransform targetTransform) {
        if (this.targetTransform == targetTransform) return;
        endDrag();
        this.targetTransform = targetTransform;
    }

    /**
     * Kept so that callers compiled against the old signature keep working — {@link Transform}
     * implements {@link ITransform}, so this is the same method, and overload resolution picks this
     * one for a {@code Transform} argument.
     */
    public void setTargetTransform(@Nullable Transform targetTransform) {
        setTargetTransform((ITransform) targetTransform);
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

    /**
     * World units per gizmo unit. Chosen so the gizmo keeps a constant size on screen, which also makes
     * every length below — ring radii, pick tolerances — a fixed fraction of the viewport.
     */
    public float getGizmoScale() {
        if (targetTransform == null || !(getScene() instanceof SceneEditor editor)) return 1f;
        var renderer = editor.scene.getRenderer();
        if (renderer == null) return 1f;
        var distance = renderer.getEyePos().distance(targetTransform.position());
        return distance * (float) Math.tan(renderer.getFov() * 0.5f * Math.PI / 180) * BASE_SCALE;
    }

    /**
     * The axis {@link Handle#SCREEN} and {@link Handle#TRACKBALL} turn about: the direction from the eye
     * to the gizmo, so the outer ring reads as a true circle wherever the gizmo sits in the frame.
     */
    public Vector3f getScreenAxis() {
        if (targetTransform != null && getScene() instanceof SceneEditor editor) {
            var renderer = editor.scene.getRenderer();
            if (renderer != null) {
                // Under an orthographic camera every ray is parallel to the view direction, so aiming the
                // ring at the eye would tilt it away from the screen plane wherever the gizmo is off centre.
                var axis = editor.scene.isUseOrtho()
                        ? new Vector3f(renderer.getLookAt()).sub(renderer.getEyePos())
                        : new Vector3f(targetTransform.position()).sub(renderer.getEyePos());
                if (axis.lengthSquared() > 1.0e-9f) return axis.normalize();
            }
        }
        return new Vector3f(0, 0, 1);
    }

    /** translate(targetPos) * rotate(orientation) * scale(screenConstant); shared by render and picking. */
    private Matrix4f gizmoMatrix() {
        if (targetTransform == null) return new Matrix4f();
        return new Matrix4f()
                .translate(targetTransform.position())
                .rotate(orientation())
                .scale(getGizmoScale());
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
        hoverHandle = isActive() && getScene() instanceof SceneEditor editor
                ? editor.getMouseRay().map(this::pickHandle).orElse(null)
                : null;
    }

    /** The handle a world-space ray would grab, or {@code null} for none. */
    @Nullable
    public Handle pickHandle(Ray worldRay) {
        if (!isActive()) return null;
        if (mode == Mode.ROTATE) return pickRotateHandle(worldRay);
        var ray = toGizmoSpace(worldRay);
        if (mode == Mode.TRANSLATE) {
            if (ray.clip(xPlaneCollider) != null) return Handle.PLANE_X;
            if (ray.clip(yPlaneCollider) != null) return Handle.PLANE_Y;
            if (ray.clip(zPlaneCollider) != null) return Handle.PLANE_Z;
        }
        if (ray.clip(xAxisCollider) != null) return Handle.AXIS_X;
        if (ray.clip(yAxisCollider) != null) return Handle.AXIS_Y;
        if (ray.clip(zAxisCollider) != null) return Handle.AXIS_Z;
        return null;
    }

    /**
     * Picks a rotation handle analytically in world space, rather than against a faceted collider in
     * gizmo space.
     *
     * <p>Two things this buys, both of which were the complaint about rotating: the grab band is an exact
     * distance from the circle, so it can be widened without a segment count fighting it; and the winner
     * is the ring <em>nearest the camera</em> instead of whichever of X, Y, Z was tested first — which is
     * why grabbing a ring that visibly crossed in front of another used to take the one behind.
     *
     * <p>The order beyond that is UE's: rings before the ball, so the inside of the gizmo is free rotation
     * and only its rim is constrained.
     */
    @Nullable
    private Handle pickRotateHandle(Ray worldRay) {
        if (targetTransform == null) return null;
        var origin = worldRay.startPos();
        var dir = worldRay.getDirection();
        if (dir.lengthSquared() < 1.0e-12f) return null;
        dir.normalize();
        var center = targetTransform.position();
        var scale = getGizmoScale();
        var tolerance = RING_PICK_TOLERANCE * scale;

        Handle best = null;
        var bestDistance = Float.MAX_VALUE;
        for (int idx = 0; idx < 3; idx++) {
            var distance = ringHitDistance(origin, dir, center, worldAxis(idx), RING_RADIUS * scale, tolerance);
            if (distance != null && distance < bestDistance) {
                bestDistance = distance;
                best = AXIS_HANDLES[idx];
            }
        }
        var screenDistance = ringHitDistance(origin, dir, center, getScreenAxis(),
                SCREEN_RING_RADIUS * scale, tolerance);
        if (screenDistance != null && screenDistance < bestDistance) {
            best = Handle.SCREEN;
        }
        if (best != null) return best;
        return rayReachesSphere(origin, dir, center, TRACKBALL_RADIUS * scale) ? Handle.TRACKBALL : null;
    }

    /**
     * Distance along a unit-direction ray at which it crosses the band around a circle, or {@code null}
     * if it misses. A ring seen exactly edge-on has no crossing and is reported as a miss, which is also
     * what it looks like.
     */
    @Nullable
    private static Float ringHitDistance(Vector3f origin, Vector3f dir, Vector3f center, Vector3f normal,
                                         float radius, float tolerance) {
        var denom = dir.dot(normal);
        if (Math.abs(denom) < 1.0e-6f) return null;
        var distance = new Vector3f(center).sub(origin).dot(normal) / denom;
        if (distance < 0) return null; // behind the camera
        var hit = new Vector3f(origin).add(new Vector3f(dir).mul(distance));
        return Math.abs(hit.distance(center) - radius) <= tolerance ? distance : null;
    }

    /** Whether a unit-direction ray passes through the sphere, i.e. lands inside its silhouette. */
    private static boolean rayReachesSphere(Vector3f origin, Vector3f dir, Vector3f center, float radius) {
        var toCenter = new Vector3f(center).sub(origin);
        var along = toCenter.dot(dir);
        if (along < 0) return false;
        return toCenter.lengthSquared() - along * along <= radius * radius;
    }

    @Override
    public boolean onMouseClick(Ray mouseRay) {
        if (!isActive()) return false;
        // the click's own ray, not the cursor's current one: they are the same in practice, and asking
        // the handle directly is what lets a test drive the gizmo without moving a real mouse
        hoverHandle = pickHandle(mouseRay);
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
        // Frozen at the grab, camera-relative handles included: the axis must not drift under the drag
        // even if the view changes, or the object would keep turning while the cursor stood still.
        dragAxis = dragHandle.isAxisAligned() ? worldAxis(dragHandle.axis) : getScreenAxis();
        dragRotateAccum = 0;
        dragPrevRaw = 0;
        dragRotateAngle = 0;
        dragStartHandleDir = null;
        dragBallStart = null;
        readoutText = null;

        var origin = worldRay.startPos();
        var dir = worldRay.getDirection();
        if (dragHandle.plane) {
            var hit = rayPlaneIntersect(origin, dir, center, dragAxis);
            dragGrabOffset = hit == null ? new Vector3f() : new Vector3f(hit).sub(center);
        } else if (dragHandle == Handle.TRACKBALL) {
            dragBallRadius = TRACKBALL_RADIUS * getGizmoScale();
            dragBallStart = ballPoint(origin, dir, center, dragBallRadius);
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
        dragBallStart = null;
        dragBallRadius = 0;
        readoutText = null;
    }

    // ---------------------------------------------------------------------------------------------
    // drag resolution (runs per frame)
    // ---------------------------------------------------------------------------------------------

    @Override
    @OnlyIn(Dist.CLIENT)
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
        var snap = UIElement.isCtrlDown();
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
        } else if (dragHandle == Handle.TRACKBALL) {
            var unitDir = new Vector3f(dir);
            if (unitDir.lengthSquared() > 1.0e-12f && dragBallStart != null) {
                unitDir.normalize();
                var current = ballPoint(origin, unitDir, dragStartPosition, dragBallRadius);
                // Absolute, not accumulated per frame: the rotation is always "from where you grabbed to
                // where you are", so dragging back to the start puts the object back exactly.
                var delta = new Quaternionf().rotationTo(dragBallStart, current);
                if (snap) delta = snapRotation(delta, SNAP_ROTATE);
                targetTransform.rotation(delta.mul(dragStartRotation, new Quaternionf()));
                readoutText = "%.1f°".formatted(Math.toDegrees(angleOf(delta)));
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
            var gizmoScale = getGizmoScale();
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
        RenderSystem.disableDepthTest();
    }

    @Override
    public void postDraw(float partialTicks) {
        RenderSystem.enableDepthTest();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        if (targetTransform == null) return;
        poseStack.pushPose();
        poseStack.mulPose(gizmoMatrix());
        drawInternal(poseStack, bufferSource, partialTicks);
        poseStack.popPose();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInternal(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        if (targetTransform == null) return;
        switch (mode) {
            case TRANSLATE -> drawTranslate(poseStack, bufferSource);
            case SCALE -> drawScale(poseStack, bufferSource);
            case ROTATE -> drawRotate(poseStack, bufferSource);
            default -> { return; }
        }
        if (bufferSource instanceof MultiBufferSource.BufferSource source) {
            source.endLastBatch();
        }
    }

    /**
     * The infinite guide line along the axis being dragged, so a translate or scale drag shows what it is
     * constrained to. Nothing is drawn for a planar or camera-relative handle: neither has a single axis,
     * and reading {@code axis} on the latter would quietly draw the Z one.
     */
    private void drawDragAxisGuide(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (dragHandle == null || dragHandle.plane || !dragHandle.isAxisAligned()) return;
        var line = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines());
        drawInfiniteAxisLine(poseStack, line, dragHandle.axis);
    }

    private void drawTranslate(PoseStack poseStack, MultiBufferSource bufferSource) {
        drawDragAxisGuide(poseStack, bufferSource);
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
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

    private void drawScale(PoseStack poseStack, MultiBufferSource bufferSource) {
        drawDragAxisGuide(poseStack, bufferSource);
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
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

    /**
     * The UE rotation widget: three axis rings, an outer ring for the screen plane, and a ball you can
     * grab anywhere inside for free rotation.
     *
     * <p>Draw order is load-bearing. Depth testing is off for the whole gizmo, so what is submitted last
     * to a buffer wins — the ball goes down first so the rings sit on top of it rather than being washed
     * out by it.
     */
    private void drawRotate(PoseStack poseStack, MultiBufferSource bufferSource) {
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
        if (isTrackballVisible()) {
            // Faint on its own so it reads as a grabbable region without hiding the model inside it, and
            // brighter under the cursor, which is the only feedback free rotation can give before it starts.
            var alpha = activeHandle() == Handle.TRACKBALL ? 0.16f : 0.05f;
            RenderBufferUtils.shapeSphere(poseStack, buffer, 0, 0, 0, TRACKBALL_RADIUS * 0.99f, 12, 24,
                    1f, 1f, 1f, alpha);
        }
        for (int idx = 0; idx < 3; idx++) {
            if (!isAxisVisible(idx)) continue;
            var c = axisColor(idx, isAxisHighlighted(idx));
            RenderBufferUtils.shapeTorus(poseStack, buffer, new Vector3f(), localAxis(idx),
                    RING_RADIUS, RING_TUBE_RADIUS, RING_SEGMENTS, RING_TUBE_SEGMENTS, c[0], c[1], c[2], c[3]);
        }
        // The outer ring and the angle indicator face the camera, not the gizmo, so they are built in
        // world space with the gizmo matrix undone.
        if (!isScreenRingVisible() && !isRotateIndicatorVisible()) return;
        poseStack.pushPose();
        poseStack.mulPose(gizmoMatrix().invert());
        if (isScreenRingVisible()) {
            drawScreenRing(poseStack, bufferSource);
        }
        if (isRotateIndicatorVisible()) {
            drawRotateIndicator(poseStack, bufferSource);
        }
        poseStack.popPose();
    }

    /** The view-facing outer ring, in world space. Rotating it turns the target in the plane of the screen. */
    private void drawScreenRing(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (targetTransform == null) return;
        var scale = getGizmoScale();
        var c = screenRingColor(activeHandle() == Handle.SCREEN);
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
        RenderBufferUtils.shapeTorus(poseStack, buffer, targetTransform.position(), getScreenAxis(),
                SCREEN_RING_RADIUS * scale, RING_TUBE_RADIUS * scale, RING_SEGMENTS, RING_TUBE_SEGMENTS,
                c[0], c[1], c[2], c[3]);
    }

    /** Draws the translucent swept-angle sector + guide lines. Expects a world-space pose. */
    private void drawRotateIndicator(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (targetTransform == null || dragStartHandleDir == null) return;
        var center = targetTransform.position();
        var radius = draggedRingRadius() * getGizmoScale();
        var u = new Vector3f(dragStartHandleDir);
        var v = new Vector3f(dragAxis).cross(u, new Vector3f());
        if (v.lengthSquared() > 1.0e-9f) v.normalize();

        var tri = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
        RenderBufferUtils.shapeSector(poseStack, tri, center, u, v, radius, 0, dragRotateAngle, ARC_SEGMENTS,
                1f, 1f, 0f, 0.35f);

        var line = bufferSource.getBuffer(LDLibRenderTypes.noDepthLines());
        var startPt = new Vector3f(center).add(new Vector3f(u).mul(radius));
        var endDir = new Vector3f(u).mul(Mth.cos(dragRotateAngle)).add(new Vector3f(v).mul(Mth.sin(dragRotateAngle)));
        var endPt = new Vector3f(center).add(endDir.mul(radius));
        RenderBufferUtils.drawLine(poseStack.last(), line, center, startPt, 1f, 1f, 1f, 0.5f, 1f, 1f, 1f, 0.5f);
        RenderBufferUtils.drawLine(poseStack.last(), line, center, endPt, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f);
    }

    private float draggedRingRadius() {
        return dragHandle == Handle.SCREEN ? SCREEN_RING_RADIUS : RING_RADIUS;
    }

    private void drawInfiniteAxisLine(PoseStack poseStack, VertexConsumer buffer, int idx) {
        var far = axisPoint(idx, 50f);
        RenderBufferUtils.drawLine(poseStack.last(), buffer, new Vector3f(far).negate(), far,
                1f, 1f, 1f, 0.6f, 1f, 1f, 1f, 0.6f);
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

    /** As {@link #axisColor}, for the outer ring: neutral grey, since it belongs to no axis. */
    private float[] screenRingColor(boolean highlight) {
        return highlight ? new float[]{1f, 1f, 0f, 1f} : new float[]{0.85f, 0.85f, 0.85f, 1f};
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

    /**
     * While a handle is dragged the others are hidden, so nothing competes with the one in use — except
     * under free rotation, where hiding the rings would take away the only reference for where the object
     * has got to.
     */
    private boolean isAxisVisible(int idx) {
        if (dragHandle == null || dragHandle == Handle.TRACKBALL) return true;
        return !dragHandle.plane && dragHandle.axis == idx;
    }

    private boolean isPlaneVisible(int idx) {
        return dragHandle == null || (dragHandle.plane && dragHandle.axis == idx);
    }

    private boolean isScreenRingVisible() {
        return mode == Mode.ROTATE
                && (dragHandle == null || dragHandle == Handle.SCREEN || dragHandle == Handle.TRACKBALL);
    }

    private boolean isTrackballVisible() {
        return mode == Mode.ROTATE && (dragHandle == null || dragHandle == Handle.TRACKBALL);
    }

    /** The swept-angle sector only exists for a drag around a ring, which is the only kind with a plane. */
    private boolean isRotateIndicatorVisible() {
        return dragHandle != null && dragStartHandleDir != null;
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

    /**
     * Where a ray grabs the trackball, as a unit vector from its centre.
     *
     * <p>A ray that misses the ball is pulled onto its silhouette rather than rejected. That is what keeps
     * free rotation continuous once the cursor leaves the gizmo: instead of the drag dying at the rim, it
     * carries on spinning about the view axis, which is the behaviour that makes it feel like a ball
     * rather than a disc. {@code dir} must be normalized.
     */
    private static Vector3f ballPoint(Vector3f origin, Vector3f dir, Vector3f center, float radius) {
        var toOrigin = new Vector3f(origin).sub(center);
        var along = toOrigin.dot(dir);
        var outside = toOrigin.lengthSquared() - radius * radius;
        var discriminant = along * along - outside;
        Vector3f point;
        if (discriminant >= 0) { // through the ball: take the near surface point
            var distance = -along - (float) Math.sqrt(discriminant);
            point = new Vector3f(origin).add(new Vector3f(dir).mul(distance)).sub(center);
        } else { // past it: the closest approach, which normalizing puts on the silhouette
            point = new Vector3f(origin).add(new Vector3f(dir).mul(-along)).sub(center);
        }
        return point.lengthSquared() < 1.0e-9f ? new Vector3f(0, 0, 1) : point.normalize();
    }

    /** The rotation angle a quaternion carries, in radians. */
    private static float angleOf(Quaternionf rotation) {
        return new AxisAngle4f().set(rotation).angle;
    }

    /** The same rotation with its angle rounded to a multiple of {@code step}, keeping its axis. */
    private static Quaternionf snapRotation(Quaternionf rotation, float step) {
        var axisAngle = new AxisAngle4f().set(rotation);
        var snapped = Math.round(axisAngle.angle / step) * step;
        if (Math.abs(snapped) < 1.0e-6f) return new Quaternionf();
        return new Quaternionf().fromAxisAngleRad(axisAngle.x, axisAngle.y, axisAngle.z, snapped);
    }

    private static void snapVector(Vector3f v, float step) {
        v.set(Math.round(v.x / step) * step, Math.round(v.y / step) * step, Math.round(v.z / step) * step);
    }

    private static String fmt(Vector3f v) {
        return "%.2f, %.2f, %.2f".formatted(v.x, v.y, v.z);
    }

    /**
     * A ring approximated by a chain of boxes.
     *
     * <p>No longer how the rotation rings are picked — that is {@link #ringHitDistance}, which is exact,
     * takes an explicit tolerance and can rank hits by distance, none of which a {@link VoxelShape} can
     * do. Kept because it is public and someone else may be building a collider out of it.
     */
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
