package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.BlockModelObject;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.TransformGizmo;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.math.ITransform;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.util.Mth;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

/**
 * The scene editor's rotation gizmo, driven with a real cursor.
 *
 * <p>Every handle is aimed at by computing a point on it in <em>world</em> space and projecting that to
 * the screen, rather than by clicking an element's bounding box: the gizmo is 3D geometry with no
 * element of its own, and its rings are circles whose bounding-box centre is the one place on them
 * nothing can be grabbed.
 *
 * <p>What this is actually protecting, beyond "rotation still rotates":
 * <ul>
 *   <li><b>The grab band is wider than the ring is drawn.</b> Aiming a little off a ring still takes it —
 *       the whole complaint about the old three-pixel lines was that you had to hit them exactly.</li>
 *   <li><b>The three ring handles stay separable</b> from the two camera-relative ones, and the ball only
 *       answers where no ring does. Pick order is the thing most likely to silently regress, because a
 *       wrong answer there still rotates something and so looks like it works.</li>
 *   <li><b>Free rotation is reversible.</b> It is computed from the grab, not accumulated per frame, so
 *       dragging back to where the drag started puts the object back. An accumulating implementation
 *       passes every other check here and fails this one.</li>
 * </ul>
 */
@LDLRegisterClient(name = "gizmo_rotate", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class GizmoRotateScenario implements UIScenario {

    private static final String EDITOR = "gizmo_editor";
    private static final String START_ROTATION = "gizmo_start_rotation";

    /** Chosen so no axis ring is anywhere near edge-on: every one of them is comfortably aimable. */
    private static final float CAMERA_YAW = 35;
    private static final float CAMERA_PITCH = 30;
    private static final float DRAG_ANGLE = (float) Math.toRadians(40);

    /**
     * The scene renders {@code lastHit} <em>after</em> it has already called back into the gizmo, so a
     * cursor move is one frame away from changing what is hovered.
     */
    private static final int HOVER_FRAMES = 3;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(40).tags("editor", "scene", "gizmo").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("gizmo_scene", ctx -> {
                    var root = new UIElement();
                    var sceneEditor = new SceneEditor();
                    sceneEditor.layout(layout -> {
                        layout.widthPercent(100);
                        layout.heightPercent(100);
                    });
                    var player = ctx.requirePlayer();
                    var origin = player.getOnPos();
                    sceneEditor.scene
                            .createScene(player.level())
                            .setTickWorld(false)
                            .setRenderedCore(List.of(origin,
                                    origin.offset(1, 0, 0), origin.offset(-1, 0, 0),
                                    origin.offset(0, 0, 1), origin.offset(0, 0, -1)));
                    // Big enough that the gizmo is tens of pixels across: every tolerance below is a
                    // fraction of the viewport, so a cramped one would make the aiming the test's own
                    // problem rather than the gizmo's.
                    root.layout(layout -> {
                        layout.width(400);
                        layout.height(340);
                    }).setId("gizmo_root");
                    root.addChildren(sceneEditor);

                    var target = new BlockModelObject();
                    target.transform().position(new Vector3f(origin.getX() + 0.5f, origin.getY() + 1.5f,
                            origin.getZ() + 0.5f));
                    sceneEditor.addSceneObject(target);
                    sceneEditor.setTransformGizmoTarget(target.transform());
                    sceneEditor.setTransformGizmoMode(TransformGizmo.Mode.ROTATE);
                    // GLOBAL keeps the ring normals on the world axes as the target turns, so a point
                    // computed for the X ring is still on the X ring after an earlier step rotated it.
                    sceneEditor.getTransformGizmo().setSpace(TransformGizmo.Space.GLOBAL);
                    ctx.put(EDITOR, sceneEditor);
                    return new ModularUI(UI.of(root), ctx.player());
                })
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the scene has a renderer", ctx -> editor(ctx).scene.getRenderer() != null)
                .step("point the camera at the gizmo", ctx -> {
                    var scene = editor(ctx).scene;
                    scene.setCameraYawAndPitch(CAMERA_YAW, CAMERA_PITCH);
                    scene.setZoom(5);
                    scene.setCenter(center(ctx));
                })
                .frames(20)
                .waitUntil("the gizmo projects onto the screen", ctx -> project(ctx, center(ctx)) != null)
                .check("the gizmo is active", ctx -> gizmo(ctx).isActive())

                .group("an axis ring", g -> g
                        .step("hover the X ring", ctx -> moveTo(ctx, ringPoint(ctx, 0, 0)))
                        .frames(HOVER_FRAMES)
                        .check("the X ring is hovered",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.AXIS_X)

                        .step("aim a little off the X ring", ctx ->
                                moveTo(ctx, ringPoint(ctx, 0, 0, TransformGizmo.RING_RADIUS + 0.05f)))
                        .frames(HOVER_FRAMES)
                        .check("a near miss still grabs the X ring",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.AXIS_X)

                        .step("press on the X ring", ctx -> {
                            rememberRotation(ctx);
                            press(ctx, ringPoint(ctx, 0, 0));
                        })
                        .frames(2)
                        .check("the X ring is being dragged",
                                ctx -> gizmo(ctx).getDragHandle() == TransformGizmo.Handle.AXIS_X)
                        .step("drag 40 degrees round the ring",
                                ctx -> drag(ctx, ringPoint(ctx, 0, DRAG_ANGLE)))
                        .frames(2)
                        .step("release", ctx -> release(ctx, ringPoint(ctx, 0, DRAG_ANGLE)))
                        .check("the drag ended", ctx -> gizmo(ctx).getDragHandle() == null)
                        .check("the target turned about X, by about the angle dragged",
                                ctx -> turnedAbout(ctx, new Vector3f(1, 0, 0))))

                .group("the screen ring", g -> g
                        .step("hover the outer ring", ctx -> moveTo(ctx, screenRingPoint(ctx, 0)))
                        .frames(HOVER_FRAMES)
                        .check("the outer ring is hovered, not an axis one",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.SCREEN)

                        .step("press on the outer ring", ctx -> {
                            rememberRotation(ctx);
                            press(ctx, screenRingPoint(ctx, 0));
                        })
                        .frames(2)
                        .step("drag 40 degrees round it", ctx -> drag(ctx, screenRingPoint(ctx, DRAG_ANGLE)))
                        .frames(2)
                        .step("release", ctx -> release(ctx, screenRingPoint(ctx, DRAG_ANGLE)))
                        .check("the target turned in the plane of the screen",
                                ctx -> turnedAbout(ctx, gizmo(ctx).getScreenAxis())))

                .group("the trackball", g -> g
                        .step("hover the middle of the gizmo", ctx -> moveTo(ctx, center(ctx)))
                        .frames(HOVER_FRAMES)
                        .check("the inside of the ball is free rotation",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.TRACKBALL)

                        .step("grab the ball", ctx -> {
                            rememberRotation(ctx);
                            press(ctx, center(ctx));
                        })
                        .frames(2)
                        .check("the ball is being dragged",
                                ctx -> gizmo(ctx).getDragHandle() == TransformGizmo.Handle.TRACKBALL)
                        .step("roll it sideways", ctx -> drag(ctx, ballPoint(ctx, 0.55f)))
                        .frames(2)
                        .check("free rotation turned the target",
                                ctx -> rotatedBy(ctx) > 15)
                        .step("drag back to where it was grabbed", ctx -> drag(ctx, center(ctx)))
                        .frames(2)
                        .check("dragging back to the grab restores the rotation",
                                ctx -> rotatedBy(ctx) < 5)
                        .step("release", ctx -> release(ctx, center(ctx)))
                        .check("the drag ended", ctx -> gizmo(ctx).getDragHandle() == null))

                .step("park the cursor off the gizmo", ctx -> moveTo(ctx, screenRingPoint(ctx, 0, 2.2f)))
                .frames(HOVER_FRAMES)
                .check("nothing is hovered outside the gizmo", ctx -> gizmo(ctx).getHoverHandle() == null)

                .step("put the cursor back on the rings for the capture",
                        ctx -> moveTo(ctx, ringPoint(ctx, 1, 0)))
                .frames(HOVER_FRAMES)
                .screenshot("01_rotate_gizmo")

                .closeScreen();
    }

    // ---- the gizmo under test ----

    private static SceneEditor editor(TestContext ctx) {
        return ctx.get(EDITOR);
    }

    private static TransformGizmo gizmo(TestContext ctx) {
        return editor(ctx).getTransformGizmo();
    }

    private static ITransform target(TestContext ctx) {
        var target = gizmo(ctx).getTargetTransform();
        if (target == null) throw new IllegalStateException("the gizmo lost its target");
        return target;
    }

    private static Vector3f center(TestContext ctx) {
        return target(ctx).position();
    }

    private static void rememberRotation(TestContext ctx) {
        ctx.put(START_ROTATION, target(ctx).rotation());
    }

    /** How far the target has turned since {@link #rememberRotation}, in degrees. */
    private static double rotatedBy(TestContext ctx) {
        return Math.toDegrees(new AxisAngle4f().set(rotationDelta(ctx)).angle);
    }

    /**
     * Whether the turn since {@link #rememberRotation} is about {@code axis}, by roughly the angle that
     * was dragged. Both halves matter: the angle alone would pass on a gizmo that grabbed the wrong ring.
     */
    private static boolean turnedAbout(TestContext ctx, Vector3f axis) {
        var delta = new AxisAngle4f().set(rotationDelta(ctx));
        var degrees = Math.toDegrees(delta.angle);
        // |dot|, not dot: turning -40 degrees about X is the same as +40 about -X, and which one a
        // quaternion decomposes to depends on the drag direction, which the camera angle decides.
        var alignment = Math.abs(new Vector3f(delta.x, delta.y, delta.z).normalize().dot(new Vector3f(axis).normalize()));
        ctx.log("turned %.1f degrees, axis alignment %.3f".formatted(degrees, alignment));
        var dragged = Math.toDegrees(DRAG_ANGLE);
        return alignment > 0.95 && Math.abs(degrees - dragged) < dragged / 2;
    }

    private static Quaternionf rotationDelta(TestContext ctx) {
        Quaternionf start = ctx.get(START_ROTATION);
        // the gizmo applies delta * start, so the delta is now * start^-1
        return new Quaternionf(target(ctx).rotation()).mul(new Quaternionf(start).invert());
    }

    // ---- aiming: world points on the handles ----

    private static Vector3f ringPoint(TestContext ctx, int axis, float angle) {
        return ringPoint(ctx, axis, angle, TransformGizmo.RING_RADIUS);
    }

    /**
     * A point on one of the three axis rings.
     *
     * <p>Angle zero is the point on the ring nearest the camera, so a hover there is on the near side of
     * the gizmo and cannot be beaten to it by a ring in front — which the pick, being nearest-first,
     * would rightly prefer.
     */
    private static Vector3f ringPoint(TestContext ctx, int axis, float angle, float radius) {
        var normal = switch (axis) {
            case 0 -> new Vector3f(1, 0, 0);
            case 1 -> new Vector3f(0, 1, 0);
            default -> new Vector3f(0, 0, 1);
        };
        return circlePoint(ctx, normal, towardsCamera(ctx, normal), angle, radius);
    }

    private static Vector3f screenRingPoint(TestContext ctx, float angle) {
        return screenRingPoint(ctx, angle, TransformGizmo.SCREEN_RING_RADIUS);
    }

    private static Vector3f screenRingPoint(TestContext ctx, float angle, float radius) {
        var normal = gizmo(ctx).getScreenAxis();
        var up = renderer(ctx).getWorldUp();
        // angle zero is screen-up, so the points are easy to reason about in a screenshot
        var reference = new Vector3f(up).sub(new Vector3f(normal).mul(up.dot(normal)));
        if (reference.lengthSquared() < 1.0e-6f) reference = new Vector3f(1, 0, 0);
        return circlePoint(ctx, normal, reference.normalize(), angle, radius);
    }

    /** A point inside the ball, {@code fraction} of the way out towards the rings, in the screen plane. */
    private static Vector3f ballPoint(TestContext ctx, float fraction) {
        return screenRingPoint(ctx, (float) (Math.PI / 2), TransformGizmo.TRACKBALL_RADIUS * fraction);
    }

    private static Vector3f circlePoint(TestContext ctx, Vector3f normal, Vector3f reference,
                                        float angle, float radius) {
        var u = new Vector3f(reference).normalize();
        var v = new Vector3f(normal).cross(u, new Vector3f()).normalize();
        var offset = new Vector3f(u).mul(Mth.cos(angle)).add(v.mul(Mth.sin(angle)));
        return new Vector3f(center(ctx)).add(offset.mul(radius * gizmo(ctx).getGizmoScale()));
    }

    /** The direction, within the plane of a ring, that points most nearly at the camera. */
    private static Vector3f towardsCamera(TestContext ctx, Vector3f normal) {
        var toEye = new Vector3f(renderer(ctx).getEyePos()).sub(center(ctx));
        var inPlane = new Vector3f(toEye).sub(new Vector3f(normal).mul(toEye.dot(normal)));
        if (inPlane.lengthSquared() < 1.0e-6f) {
            // looking straight down the ring's axis: any direction in its plane will do
            inPlane = Math.abs(normal.x) < 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            inPlane.cross(normal);
        }
        return inPlane.normalize();
    }

    private static com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer renderer(TestContext ctx) {
        var renderer = editor(ctx).scene.<com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer>getRenderer();
        if (renderer == null) throw new IllegalStateException("the scene has no renderer");
        return renderer;
    }

    // ---- driving the cursor at a world point ----

    /**
     * A world point as a gui-scaled screen point.
     *
     * <p>Deliberately not {@code SceneEditor#project}: that one reads the modelview, projection and
     * viewport straight out of the live GL state, so it is only meaningful <em>inside</em> the scene's
     * own render pass. Called from a scenario step it silently hands back the caller's own x and y,
     * which is exactly how the first version of this test moved the cursor off screen and then reported
     * that nothing was hovered. Recomputed here from the camera the scene was set up with, which is the
     * same perspective {@code setupCamera} builds: {@code lookAt(eye, target, worldUp)} and a vertical
     * field of view over the element's aspect ratio.
     */
    private static Vector2f project(TestContext ctx, Vector3f world) {
        var scene = editor(ctx).scene;
        var renderer = renderer(ctx);
        var eye = renderer.getEyePos();
        var forward = new Vector3f(renderer.getLookAt()).sub(eye);
        var right = new Vector3f();
        var up = new Vector3f();
        if (forward.lengthSquared() < 1.0e-9f) return null;
        forward.normalize().cross(renderer.getWorldUp(), right);
        if (right.lengthSquared() < 1.0e-9f) return null;
        right.normalize().cross(forward, up);
        up.normalize();

        var offset = new Vector3f(world).sub(eye);
        var depth = offset.dot(forward);
        if (depth <= 1.0e-4f) return null; // behind the camera
        var width = scene.getContentWidth();
        var height = scene.getPaddingHeight();
        if (width <= 0 || height <= 0) return null;
        var tanHalfFov = (float) Math.tan(Math.toRadians(renderer.getFov() * 0.5));
        var ndcX = offset.dot(right) / (depth * tanHalfFov * (width / height));
        var ndcY = offset.dot(up) / (depth * tanHalfFov);
        return new Vector2f(scene.getContentX() + (ndcX * 0.5f + 0.5f) * width,
                scene.getContentY() + (0.5f - ndcY * 0.5f) * height);
    }

    private static Vector2f require(TestContext ctx, Vector3f world) {
        var screen = project(ctx, world);
        if (screen == null) throw new IllegalStateException("could not project " + world + " onto the screen");
        return screen;
    }

    private static void moveTo(TestContext ctx, Vector3f world) {
        var screen = require(ctx, world);
        ctx.input().moveTo(screen.x, screen.y);
    }

    private static void press(TestContext ctx, Vector3f world) {
        var screen = require(ctx, world);
        ctx.input().mouseDown(screen.x, screen.y, Keys.MOUSE_LEFT);
    }

    private static void drag(TestContext ctx, Vector3f world) {
        var screen = require(ctx, world);
        ctx.input().dragTo(screen.x, screen.y, Keys.MOUSE_LEFT);
    }

    private static void release(TestContext ctx, Vector3f world) {
        var screen = require(ctx, world);
        ctx.input().mouseUp(screen.x, screen.y, Keys.MOUSE_LEFT);
    }
}
