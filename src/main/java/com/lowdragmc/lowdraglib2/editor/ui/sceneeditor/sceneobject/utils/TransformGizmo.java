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
 *   <li>the three coloured {@linkplain Handle#AXIS_X axis rings}, which constrain the turn to one axis.
 *       Only the half of each on the near side of the ball is drawn — and only that half can be grabbed —
 *       so they read as three separate arcs on a sphere rather than three circles crossing each other
 *       twice, which is how you tell at a glance which one turns which way;</li>
 *   <li>the outer {@linkplain Handle#SCREEN screen ring}, which turns the target in the plane of the screen —
 *       about the eye-to-gizmo direction, frozen when the drag starts;</li>
 *   <li>the {@linkplain Handle#TRACKBALL ball} filling the rest of the gizmo, where a drag rolls the target
 *       freely with the grabbed point following the cursor.</li>
 * </ul>
 * A faint circle marks the ball's silhouette, which is the line the three arcs end on.
 * <p>
 * The rings are drawn as tubes rather than lines and picked analytically against an explicit tolerance that is
 * wider than they are drawn, because a line three pixels across is not something anyone can aim at.
 *
 * <h2>Moving</h2>
 * {@link Mode#TRANSLATE} has the three arrows, three {@linkplain Handle#PLANE_X squares} that slide the
 * target in one of the axis planes, and a {@linkplain Handle#FREE centre box} that slides it in the plane of
 * the screen. All four kinds of slide are the same operation — keep the point that was grabbed under the
 * cursor, on a plane fixed at the grab — differing only in which plane that is.
 *
 * <h2>Scaling</h2>
 * {@link Mode#SCALE} mirrors it: three axis boxes, three squares that scale the two axes they span, and a
 * {@linkplain Handle#UNIFORM centre box} that drives all three at once. The squares and the centre multiply
 * where an axis handle adds, so a target that was not square keeps the shape it was. What each is measured
 * along is the line it belongs to: a square's own diagonal, and for the centre — which has no axis at all —
 * the screen's diagonal, where dragging up and right grows and back down and left shrinks.
 *
 * <p>The squares sit well out along their axes rather than hugging the origin, so that aiming at one is not
 * a contest between it, the centre box and two arrows within the same few pixels.
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
     * <p>{@link #SCREEN}, {@link #TRACKBALL}, {@link #UNIFORM} and {@link #FREE} have no axis of their
     * own — the first two work about the camera, the last two drive everything at once — so {@link #axis}
     * is {@code -1} for them and {@link #isAxisAligned()} is how to ask rather than comparing the number.
     *
     * <p>{@link #plane} means "this drag is resolved against a plane" for {@link Mode#TRANSLATE} and
     * {@link #FREE}, and "this handle owns the two axes that are <em>not</em> {@link #axis}" for
     * {@link Mode#SCALE}. The flag is the same; what the gizmo does with it depends on the mode, which is
     * why {@link #dragAxisFor} takes the mode into account and nothing else should read it raw.
     */
    public enum Handle {
        AXIS_X(0, false), AXIS_Y(1, false), AXIS_Z(2, false),
        PLANE_X(0, true), PLANE_Y(1, true), PLANE_Z(2, true),
        /** The outer ring: rotate in the plane of the screen, about the camera's view direction. */
        SCREEN(-1, false),
        /** Anywhere inside the ball: free rotation, the grabbed point following the cursor. */
        TRACKBALL(-1, false),
        /** The centre box in {@link Mode#SCALE}: scale all three axes at once, keeping the proportions. */
        UNIFORM(-1, false),
        /** The centre box in {@link Mode#TRANSLATE}: move freely in the plane of the screen. */
        FREE(-1, true);

        public final int axis;      // 0=X, 1=Y, 2=Z, -1 = no single axis
        public final boolean plane; // true = planar handle, false = axis handle

        Handle(int axis, boolean plane) { this.axis = axis; this.plane = plane; }

        /** Whether this handle is tied to one of the gizmo's three axes rather than to all of them or the camera. */
        public boolean isAxisAligned() { return axis >= 0; }
    }

    /** The axis handles by axis index, so a loop over X, Y, Z does not need a switch to name its answer. */
    private static final Handle[] AXIS_HANDLES = {Handle.AXIS_X, Handle.AXIS_Y, Handle.AXIS_Z};

    // gizmo geometry, authored in gizmo-units (a constant screen scale is applied on top)
    private static final float BASE_SCALE = 0.23f;
    /** How far out the arrow tips and scale boxes sit, and so where to aim to grab one. */
    public static final float AXIS_LENGTH = 1.0f;
    private static final float SHAFT_RADIUS = 0.02f;
    private static final float ARROW_RADIUS = 0.07f;
    private static final float ARROW_HEIGHT = 0.22f;
    /**
     * The planar handles' square, as a fraction of an axis. Set well out from the origin: it used to
     * start at 0.12, close enough to the centre box that aiming at one and getting the other was a
     * coin toss, and close enough to the axes that the three of them read as one cluster.
     */
    public static final float PLANE_MIN = 0.3f;
    public static final float PLANE_MAX = 0.55f;
    /** The middle of a planar handle's square, which is where to aim to grab one. */
    public static final float PLANE_CENTRE = (PLANE_MIN + PLANE_MAX) / 2;
    /** How far outside a drawn box or square it can still be grabbed — the forgiveness the rings get too. */
    private static final float HANDLE_PICK_MARGIN = 0.04f;
    private static final float SCALE_SHAFT_LENGTH = 0.9f;
    private static final float SCALE_BOX_HALF = 0.08f;
    /** Half the drawn width of the centre box; a little larger than an axis one. */
    private static final float CENTRE_BOX_HALF = 0.1f;
    /**
     * How far a factor-based scale drag — a square or the centre box — may shrink the target. Not zero:
     * one pixel further and the factor goes negative and mirrors every axis it drives at once, which is
     * not what dragging a handle inwards is asking for.
     */
    private static final float MIN_SCALE_FACTOR = 0.01f;
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
    /**
     * How far past the ball's silhouette an axis ring keeps being drawn and grabbed, as a cosine against
     * the view axis. It is what makes the rings read as three separable arcs instead of three circles
     * that cross each other twice: only the half on the near side of the ball is there at all.
     *
     * <p>Slightly more than exactly half, on purpose. A ring seen face-on lies <em>on</em> the silhouette
     * all the way round — no half of it is the near one — and a cut at exactly zero would halve it on
     * whichever side rounding fell, flickering as the camera moved. This widens the arc smoothly to the
     * whole circle as a ring turns to face the viewer, which is also what it should look like.
     */
    private static final float RING_FRONT_BIAS = 0.05f;
    /** The faint circle around the ball, relative to {@link #RING_TUBE_RADIUS} and as an alpha. */
    private static final float BALL_OUTLINE_THICKNESS = 0.5f;
    private static final float BALL_OUTLINE_ALPHA = 0.22f;
    /**
     * How far past the gizmo's own origin a pick segment is made to run, in gizmo units. Every collider
     * is well inside it — the axes reach 1.2 — and it is measured from the origin rather than from the
     * ray's start, which can be arbitrarily far away.
     */
    private static final float PICK_REACH = 4f;
    private static final int RING_SEGMENTS = 64;
    private static final int RING_TUBE_SEGMENTS = 6;
    private static final int ARC_SEGMENTS = 48;
    private static final int SHAFT_SEGMENTS = 12;

    // snapping increments (Ctrl held)
    private static final float SNAP_TRANSLATE = 0.25f;
    private static final float SNAP_SCALE = 0.25f;
    private static final float SNAP_ROTATE = (float) Math.toRadians(15);

    private static final VoxelShape xAxisCollider = Shapes.box(0, -0.1, -0.1, 1.2, 0.1, 0.1);
    private static final VoxelShape yAxisCollider = Shapes.box(-0.1, 0, -0.1, 0.1, 1.2, 0.1);
    private static final VoxelShape zAxisCollider = Shapes.box(-0.1, -0.1, 0, 0.1, 0.1, 1.2);
    // The planar handles: a thin slab, a square's width across, sitting where drawPlaneQuad draws one.
    private static final float PLANE_SLAB = 0.02f;
    private static final float PLANE_LOW = PLANE_MIN - HANDLE_PICK_MARGIN;
    private static final float PLANE_HIGH = PLANE_MAX + HANDLE_PICK_MARGIN;
    private static final VoxelShape xPlaneCollider = Shapes.box(-PLANE_SLAB, PLANE_LOW, PLANE_LOW, PLANE_SLAB, PLANE_HIGH, PLANE_HIGH);
    private static final VoxelShape yPlaneCollider = Shapes.box(PLANE_LOW, -PLANE_SLAB, PLANE_LOW, PLANE_HIGH, PLANE_SLAB, PLANE_HIGH);
    private static final VoxelShape zPlaneCollider = Shapes.box(PLANE_LOW, PLANE_LOW, -PLANE_SLAB, PLANE_HIGH, PLANE_HIGH, PLANE_SLAB);
    /**
     * The centre handle. Wider than {@link #CENTRE_BOX_HALF} draws it, in the same spirit as the rings'
     * pick tolerance, and it has to be tested before the axes: all three of their colliders reach back to
     * the origin and so cover the very pixels the centre box is drawn on.
     */
    private static final float CENTRE_PICK_HALF = CENTRE_BOX_HALF + HANDLE_PICK_MARGIN;
    private static final VoxelShape centreCollider = Shapes.box(
            -CENTRE_PICK_HALF, -CENTRE_PICK_HALF, -CENTRE_PICK_HALF,
            CENTRE_PICK_HALF, CENTRE_PICK_HALF, CENTRE_PICK_HALF);

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
     *
     * <p>The projection is asked how much world a viewport half-height is worth rather than worked out
     * here from the field of view, because under an orthographic camera it is not worth
     * {@code distance * tan(fov / 2)} at all — and there the eye tends to sit a tenth of a block from
     * the target, so a gizmo scaled that way came out a few thousandths of a block across.
     */
    public float getGizmoScale() {
        if (targetTransform == null || !(getScene() instanceof SceneEditor editor)) return 1f;
        var renderer = editor.scene.getRenderer();
        if (renderer == null) return 1f;
        var distance = renderer.getEyePos().distance(targetTransform.position());
        var scale = renderer.getViewHalfHeight(distance) * BASE_SCALE;
        // A camera can honestly answer zero — an orthographic one whose box was never sized does — and
        // {@link #gizmoMatrix()} would then be singular, leaving both drawing and picking working on
        // non-finite numbers instead of on a very small gizmo.
        return scale > 1.0e-6f ? scale : 1f;
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
                var axis = renderer.isOrtho()
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

    /**
     * The world direction a drag on {@code handle} is resolved against — which is a line to measure along
     * for the handles that move or scale one thing, and the normal of a plane to slide in for the ones
     * that move in two.
     *
     * <p>The mode is part of the question for a planar handle, and this is the only place that knows it:
     * translating on the XY square is a slide in that plane, so the answer is its normal, while scaling on
     * it runs out along the square's own diagonal, so the answer is that line.
     */
    private Vector3f dragAxisFor(Handle handle) {
        if (handle == Handle.UNIFORM) return getScreenDiagonal();
        if (handle == Handle.FREE) return getScreenAxis();
        if (handle.plane) {
            return mode == Mode.SCALE ? planeDiagonal(handle.axis) : worldAxis(handle.axis);
        }
        if (handle.isAxisAligned()) return worldAxis(handle.axis);
        return getScreenAxis();
    }

    /** The diagonal of a planar handle's square: the line a scale drag on it is measured along. */
    private Vector3f planeDiagonal(int idx) {
        var diagonal = new Vector3f();
        for (int other = 0; other < 3; other++) {
            if (other != idx) diagonal.add(worldAxis(other));
        }
        return diagonal.normalize();
    }

    /**
     * The direction a {@link Handle#UNIFORM} drag is measured along: the screen's up-and-right diagonal, so
     * dragging out from the gizmo grows the target and back across it shrinks it.
     *
     * <p>A fixed direction rather than the one the box was grabbed from. The centre box is a few pixels
     * across, so every grab lands near enough its middle that the direction away from there is noise, and a
     * drag that grew or shrank depending on which pixel started it is not something anyone could aim.
     *
     * <p>Public for the same reason as {@link #getScreenAxis()}: it is where a caller aims to drive the
     * handle, and recomputing it outside would be guessing at a convention rather than reading it.
     */
    public Vector3f getScreenDiagonal() {
        var forward = new Vector3f(0, 0, 1);
        var up = new Vector3f(0, 1, 0);
        if (getScene() instanceof SceneEditor editor) {
            var renderer = editor.scene.getRenderer();
            if (renderer != null) {
                // the camera's own view direction, not the eye-to-gizmo one: this is a screen-space
                // direction, and off-centre it must still lie in the plane of the screen
                var look = new Vector3f(renderer.getLookAt()).sub(renderer.getEyePos());
                if (look.lengthSquared() > 1.0e-9f) {
                    forward = look.normalize();
                    up = new Vector3f(renderer.getWorldUp());
                }
            }
        }
        var right = new Vector3f(forward).cross(up);
        // looking straight along world up: with no horizon to take right from, any direction across the view will do
        if (right.lengthSquared() < 1.0e-9f) right = perpendicular(forward);
        right.normalize();
        var screenUp = new Vector3f(right).cross(forward).normalize();
        return right.add(screenUp).normalize();
    }

    // ---------------------------------------------------------------------------------------------
    // picking
    // ---------------------------------------------------------------------------------------------

    /**
     * Transform a world-space ray into gizmo-local space (where the colliders live).
     *
     * <p>The segment is re-cut to reach past the origin, rather than extended by the fixed hundred units
     * {@link Ray#toInfinite()} gives. The transform divides by the gizmo's screen scale, so how far away
     * the ray <em>starts</em> in gizmo units depends on how small the gizmo is: an orthographic camera
     * fires from hundreds of blocks back to keep its rays parallel, which lands the start thousands of
     * gizmo-units out, and a hundred units of reach from there stops well short of every collider. That
     * picks nothing at all, everywhere, which reads as the gizmo being dead rather than as a ray that was
     * too short.
     */
    private Ray toGizmoSpace(Ray ray) {
        var local = ray.transform(gizmoMatrix().invert());
        var direction = local.getDirection();
        if (direction.lengthSquared() < 1.0e-12f) return local;
        return Ray.create(local.startPos(), direction, local.startPos().length() + PICK_REACH);
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
        // The centre first: every axis collider reaches back to the origin, so the box drawn there only
        // exists at all if it is asked about before them.
        var centre = centreHandle();
        if (centre != null && ray.clip(centreCollider) != null) return centre;
        if (ray.clip(xPlaneCollider) != null) return Handle.PLANE_X;
        if (ray.clip(yPlaneCollider) != null) return Handle.PLANE_Y;
        if (ray.clip(zPlaneCollider) != null) return Handle.PLANE_Z;
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

        var viewAxis = getScreenAxis();
        Handle best = null;
        var bestDistance = Float.MAX_VALUE;
        for (int idx = 0; idx < 3; idx++) {
            var distance = ringHitDistance(origin, dir, center, worldAxis(idx), RING_RADIUS * scale, tolerance);
            if (distance == null || distance >= bestDistance) continue;
            // Only the arc that is drawn can be grabbed. Without this the far half of a ring is still
            // live behind the ball, which is where "I clicked the ring I could see and got another one"
            // came from — and it is the whole reason only half of each is drawn.
            var hit = new Vector3f(origin).add(new Vector3f(dir).mul(distance));
            if (!isRingFrontFacing(hit, center, viewAxis)) continue;
            bestDistance = distance;
            best = AXIS_HANDLES[idx];
        }
        // The screen ring is exempt: it faces the camera, so every point of it is exactly on the
        // silhouette and the near-half question does not apply.
        var screenDistance = ringHitDistance(origin, dir, center, viewAxis,
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

    /**
     * Whether a point on a ring is on the half of the ball facing the camera — the half that is drawn.
     * The cut is {@link #RING_FRONT_BIAS}, the same one {@link #frontArcHalfSweep} draws to, so what is
     * grabbable and what is visible are the same set rather than two things that agree by eye.
     */
    private static boolean isRingFrontFacing(Vector3f point, Vector3f center, Vector3f viewAxis) {
        var radial = new Vector3f(point).sub(center);
        if (radial.lengthSquared() < 1.0e-12f) return true;
        return radial.normalize().dot(viewAxis) < RING_FRONT_BIAS;
    }

    /**
     * The direction inside a ring's plane that points most nearly at the camera, <b>not</b> normalized:
     * its length is how much of the view axis lies in that plane, which is what says how side-on the ring
     * is. The arc is centred on this direction and {@link #frontArcHalfSweep} widens it by that length.
     */
    private static Vector3f towardEyeInPlane(Vector3f normal, Vector3f viewAxis) {
        var toEye = new Vector3f(viewAxis).negate(); // the view axis runs eye → gizmo
        return toEye.sub(new Vector3f(normal).mul(toEye.dot(normal)));
    }

    /**
     * Half the arc of a ring that faces the camera, in radians — π/2 for a ring seen edge-on, growing to
     * π (the whole circle) as it turns to face the viewer and no half of it is nearer than the other.
     */
    private static float frontArcHalfSweep(float inPlaneLength) {
        if (inPlaneLength <= RING_FRONT_BIAS) return (float) Math.PI;
        return (float) Math.acos(-RING_FRONT_BIAS / inPlaneLength);
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
        dragAxis = dragAxisFor(dragHandle);
        dragRotateAccum = 0;
        dragPrevRaw = 0;
        dragRotateAngle = 0;
        dragStartHandleDir = null;
        dragBallStart = null;
        readoutText = null;

        var origin = worldRay.startPos();
        var dir = worldRay.getDirection();
        if (isPlaneSlide()) {
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
        } else { // anything measured along a line: axis translate, axis / planar / uniform scale
            var closest = Vector3fHelper.closestPointOnLine(origin, dir, center, dragAxis);
            dragStartParam = new Vector3f(closest).sub(center).dot(dragAxis);
        }
    }

    /**
     * Whether the drag in progress slides along a plane rather than along a line. Every planar handle
     * does in {@link Mode#TRANSLATE}, and none does in {@link Mode#SCALE}, where the square is a handle
     * for two axes rather than a surface to move on.
     */
    private boolean isPlaneSlide() {
        return dragHandle != null && dragHandle.plane && mode != Mode.SCALE;
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
        var handle = dragHandle;
        if (targetTransform == null || handle == null) return;
        var origin = worldRay.startPos();
        var dir = worldRay.getDirection();
        var snap = UIElement.isCtrlDown();
        var changed = false;

        if (isPlaneSlide()) {
            var hit = rayPlaneIntersect(origin, dir, dragStartPosition, dragAxis);
            if (hit != null) {
                var newPos = new Vector3f(hit).sub(dragGrabOffset);
                if (snap) snapVector(newPos, SNAP_TRANSLATE);
                targetTransform.position(newPos);
                readoutText = fmt(new Vector3f(newPos).sub(dragStartPosition));
                changed = true;
            }
        } else if (handle == Handle.TRACKBALL) {
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
            Vector3f newScale;
            if (handle == Handle.UNIFORM || handle.plane) {
                // A factor where an axis drag is an offset: axes that started at different scales have to
                // keep their proportions, which is the whole reason to grab a square or the centre rather
                // than an axis. The planar handles leave their own axis alone; the centre takes all three.
                var factor = 1 + delta;
                if (snap) factor = Math.round(factor / SNAP_SCALE) * SNAP_SCALE;
                factor = Math.max(factor, MIN_SCALE_FACTOR);
                newScale = new Vector3f(dragStartScale);
                for (int i = 0; i < 3; i++) {
                    if (i != handle.axis) newScale.setComponent(i, dragStartScale.get(i) * factor);
                }
                readoutText = "×%.2f  %s".formatted(factor, fmt(newScale));
            } else {
                var idx = handle.axis;
                var newComp = dragStartScale.get(idx) + delta;
                if (snap) newComp = Math.round(newComp / SNAP_SCALE) * SNAP_SCALE;
                newScale = new Vector3f(dragStartScale).setComponent(idx, newComp);
                readoutText = fmt(newScale);
            }
            targetTransform.localScale(newScale);
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
        drawPlaneHandles(poseStack, buffer);
        drawCentreBox(poseStack, buffer);
    }

    private void drawPlaneHandles(PoseStack poseStack, VertexConsumer buffer) {
        for (int idx = 0; idx < 3; idx++) {
            if (!isPlaneVisible(idx)) continue;
            var c = axisColor(idx, isPlaneHighlighted(idx));
            drawPlaneQuad(poseStack, buffer, idx, c);
        }
    }

    /**
     * The box at the origin: free movement in {@link Mode#TRANSLATE}, uniform scale in {@link Mode#SCALE}.
     * Drawn last, so it sits on top of the three shafts, which all start underneath it.
     */
    private void drawCentreBox(PoseStack poseStack, VertexConsumer buffer) {
        var centre = centreHandle();
        if (centre == null || (dragHandle != null && dragHandle != centre)) return;
        var c = neutralColor(activeHandle() == centre);
        RenderBufferUtils.drawCubeFace(poseStack, buffer,
                -CENTRE_BOX_HALF, -CENTRE_BOX_HALF, -CENTRE_BOX_HALF,
                CENTRE_BOX_HALF, CENTRE_BOX_HALF, CENTRE_BOX_HALF,
                c[0], c[1], c[2], c[3], true);
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
        drawPlaneHandles(poseStack, buffer);
        drawCentreBox(poseStack, buffer);
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
        // The view direction in the gizmo's own space, so an arc can be cut against it without the ring
        // geometry having to leave the space its normals are written in.
        var localView = new Quaternionf(orientation()).conjugate().transform(new Vector3f(getScreenAxis()));
        for (int idx = 0; idx < 3; idx++) {
            if (!isAxisVisible(idx)) continue;
            var c = axisColor(idx, isAxisHighlighted(idx));
            var normal = localAxis(idx);
            if (isRingWhole(idx)) {
                RenderBufferUtils.shapeTorus(poseStack, buffer, new Vector3f(), normal,
                        RING_RADIUS, RING_TUBE_RADIUS, RING_SEGMENTS, RING_TUBE_SEGMENTS, c[0], c[1], c[2], c[3]);
            } else {
                var toEye = towardEyeInPlane(normal, localView);
                var half = frontArcHalfSweep(toEye.length());
                RenderBufferUtils.shapeTorusArc(poseStack, buffer, new Vector3f(), normal, toEye,
                        RING_RADIUS, RING_TUBE_RADIUS, -half, half * 2, RING_SEGMENTS, RING_TUBE_SEGMENTS,
                        c[0], c[1], c[2], c[3]);
            }
        }
        // The ball's outline, the outer ring and the angle indicator all face the camera rather than the
        // gizmo, so they are built in world space with the gizmo matrix undone.
        poseStack.pushPose();
        poseStack.mulPose(gizmoMatrix().invert());
        drawBallOutline(poseStack, bufferSource);
        if (isScreenRingVisible()) {
            drawScreenRing(poseStack, bufferSource);
        }
        if (isRotateIndicatorVisible()) {
            drawRotateIndicator(poseStack, bufferSource);
        }
        poseStack.popPose();
    }

    /**
     * The faint circle around the ball, in world space: the sphere's own silhouette, and the line every
     * axis arc ends on. Without it the three arcs are three strokes floating in nothing, and which sphere
     * they are halves of is left to the reader.
     *
     * <p>Drawn through a drag, unlike the ball itself, which is hidden as soon as one starts: it is the
     * reference the arcs are cut against, and a turn is far easier to read against a circle that stays put.
     */
    private void drawBallOutline(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (targetTransform == null) return;
        var scale = getGizmoScale();
        var buffer = bufferSource.getBuffer(LDLibRenderTypes.positionColorNoDepth());
        RenderBufferUtils.shapeTorus(poseStack, buffer, targetTransform.position(), getScreenAxis(),
                RING_RADIUS * scale, RING_TUBE_RADIUS * BALL_OUTLINE_THICKNESS * scale,
                RING_SEGMENTS, RING_TUBE_SEGMENTS, 1f, 1f, 1f, BALL_OUTLINE_ALPHA);
    }

    /** The view-facing outer ring, in world space. Rotating it turns the target in the plane of the screen. */
    private void drawScreenRing(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (targetTransform == null) return;
        var scale = getGizmoScale();
        var c = neutralColor(activeHandle() == Handle.SCREEN);
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

    /**
     * As {@link #axisColor}, for the handles that belong to no single axis — the outer rotation ring and
     * the centre scale box: neutral grey, and the same yellow as everything else under the cursor.
     */
    private float[] neutralColor(boolean highlight) {
        return highlight ? new float[]{1f, 1f, 0f, 1f} : new float[]{0.85f, 0.85f, 0.85f, 1f};
    }

    private Handle activeHandle() {
        return dragHandle != null ? dragHandle : hoverHandle;
    }

    /**
     * The handle drawn at the origin for the current mode, or {@code null} where there is none. Rotation
     * has the trackball there instead, which is picked against the ball rather than against a box.
     */
    @Nullable
    private Handle centreHandle() {
        return switch (mode) {
            case TRANSLATE -> Handle.FREE;
            case SCALE -> Handle.UNIFORM;
            default -> null;
        };
    }

    /**
     * Lighting up an axis says "this is what the handle under your cursor will move". The centre box in
     * scale mode drives all three, and a scale square drives the two that are not its own — which is the
     * only way to tell the XY square from the XZ one before committing to a drag.
     */
    private boolean isAxisHighlighted(int idx) {
        var h = activeHandle();
        if (h == null) return false;
        if (h == Handle.UNIFORM) return true;
        if (h.plane) return mode == Mode.SCALE && h.axis != idx;
        return h.axis == idx;
    }

    private boolean isPlaneHighlighted(int idx) {
        var h = activeHandle();
        return h != null && h.plane && h.axis == idx;
    }

    /**
     * While a handle is dragged the others are hidden, so nothing competes with the one in use — except
     * under free rotation, where hiding the rings would take away the only reference for where the object
     * has got to, and under a uniform or planar scale, where the axes <em>are</em> what is being dragged.
     */
    private boolean isAxisVisible(int idx) {
        if (dragHandle == null || dragHandle == Handle.TRACKBALL || dragHandle == Handle.UNIFORM) return true;
        if (dragHandle.plane) return mode == Mode.SCALE && dragHandle.axis != idx;
        return dragHandle.axis == idx;
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

    /**
     * Whether a ring is drawn all the way round rather than as its near-side arc. Only the one being
     * turned is: it is then the only ring on screen, the swept-angle sector is read against the circle it
     * belongs to, and the drag can carry the grabbed point round behind the ball. Free rotation leaves
     * the arcs alone — all three stay up as a reference there, and having them jump to circles the moment
     * a drag started would be a change of picture for no reason.
     */
    private boolean isRingWhole(int idx) {
        return dragHandle != null && dragHandle.isAxisAligned() && dragHandle.axis == idx;
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
