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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

/**
 * The scene editor's scale gizmo — the three axis boxes and the centre box that drives all three — driven
 * with a real cursor. Aiming works as it does in {@link GizmoRotateScenario}: a point on the handle is
 * computed in world space and projected onto the screen, because the gizmo is 3D geometry with no element
 * of its own to click.
 *
 * <p>What this protects, beyond "scaling still scales":
 * <ul>
 *   <li><b>The centre box wins at the centre.</b> All three axis colliders reach back to the origin, so
 *       the centre handle only exists if it is tested first. Get that order wrong and a click in the
 *       middle still scales something, along whichever axis happened to be tested first, which looks
 *       close enough to working to survive being looked at.</li>
 *   <li><b>Uniform means proportional.</b> The target starts deliberately non-square: a centre drag has to
 *       multiply the three axes, not add the same amount to each. An implementation that added would pass
 *       every check here that only asked "did all three change".</li>
 *   <li><b>A centre drag is reversible.</b> It is measured from the grab rather than accumulated per
 *       frame, so dragging back to where it started restores the scale exactly.</li>
 * </ul>
 */
@LDLRegisterClient(name = "gizmo_scale", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class GizmoScaleScenario implements UIScenario {

    private static final String EDITOR = "gizmo_editor";
    private static final String START_SCALE = "gizmo_start_scale";
    private static final String SCREEN_SIZE = "gizmo_screen_size";

    /** As in the rotate scenario: an angle where none of the three axes is anywhere near edge-on. */
    private static final float CAMERA_YAW = 35;
    private static final float CAMERA_PITCH = 30;

    /**
     * Not square, so that a centre drag multiplying and a centre drag adding give different answers.
     * Held by the check below: whatever the drag does, x:y:z has to come back out as 1:2:0.5.
     */
    private static final Vector3f START_SCALE_VALUE = new Vector3f(1f, 2f, 0.5f);

    /**
     * How far to drag, in gizmo units — the same units the gizmo measures a scale drag in, so an axis drag
     * of this much adds it to that component and a centre drag multiplies by {@code 1 + } it.
     */
    private static final float DRAG_DISTANCE = 0.5f;
    private static final float TOLERANCE = 0.15f;

    /**
     * How far sideways the camera is panned for the off-axis pick check, in blocks. Far enough out that
     * a ray which only approximates the parallel one misses by more than a handle is thick, and still
     * inside the ortho box, whose half-width is {@code range * zoom} = 5.
     */
    private static final float PAN_BLOCKS = 3.5f;

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
        s.openModularUI("gizmo_scale_scene", ctx -> {
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
                    // Big enough that the gizmo is tens of pixels across: every tolerance here is a
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
                    target.transform().localScale(new Vector3f(START_SCALE_VALUE));
                    sceneEditor.addSceneObject(target);
                    sceneEditor.setTransformGizmoTarget(target.transform());
                    sceneEditor.setTransformGizmoMode(TransformGizmo.Mode.SCALE);
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

                .group("an axis box", g -> g
                        .step("hover the X box", ctx -> moveTo(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH)))
                        .frames(HOVER_FRAMES)
                        .check("the X box is hovered",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.AXIS_X)

                        .step("press on the X box", ctx -> {
                            rememberScale(ctx);
                            press(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH));
                        })
                        .frames(2)
                        .check("the X box is being dragged",
                                ctx -> gizmo(ctx).getDragHandle() == TransformGizmo.Handle.AXIS_X)
                        .step("drag it outwards along X", ctx ->
                                drag(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH + DRAG_DISTANCE)))
                        .frames(2)
                        .step("release", ctx ->
                                release(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH + DRAG_DISTANCE)))
                        .check("the drag ended", ctx -> gizmo(ctx).getDragHandle() == null)
                        .check("only X grew, by about the distance dragged", ctx -> {
                            var start = startScale(ctx);
                            var now = scale(ctx);
                            ctx.log("scale %s -> %s".formatted(fmt(start), fmt(now)));
                            return near(now.x, start.x + DRAG_DISTANCE)
                                    && near(now.y, start.y) && near(now.z, start.z);
                        })
                        .step("put the scale back", ctx -> target(ctx).localScale(new Vector3f(START_SCALE_VALUE))))

                .group("a planar square", g -> g
                        .step("hover the square that spans Y and Z", ctx -> moveTo(ctx, planePoint(ctx, 0)))
                        .frames(HOVER_FRAMES)
                        .check("the X planar handle is hovered",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.PLANE_X)

                        .step("press on it", ctx -> {
                            rememberScale(ctx);
                            press(ctx, planePoint(ctx, 0));
                        })
                        .frames(2)
                        .check("the X planar handle is being dragged",
                                ctx -> gizmo(ctx).getDragHandle() == TransformGizmo.Handle.PLANE_X)
                        .step("drag out along the square's own diagonal",
                                ctx -> drag(ctx, planeDragPoint(ctx, 0, DRAG_DISTANCE)))
                        .frames(2)
                        .step("release", ctx -> release(ctx, planeDragPoint(ctx, 0, DRAG_DISTANCE)))
                        .check("the two axes it spans scaled together and its own did not", ctx -> {
                            var start = startScale(ctx);
                            var now = scale(ctx);
                            var expected = 1 + DRAG_DISTANCE;
                            ctx.log("scale %s -> %s".formatted(fmt(start), fmt(now)));
                            return near(now.x, start.x)
                                    && near(now.y / start.y, expected)
                                    && near(now.z / start.z, expected);
                        })
                        .step("put the scale back", ctx -> target(ctx).localScale(new Vector3f(START_SCALE_VALUE))))

                .group("the centre box", g -> g
                        .step("hover the middle of the gizmo", ctx -> moveTo(ctx, center(ctx)))
                        .frames(HOVER_FRAMES)
                        .check("the middle is the uniform handle, not an axis one",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.UNIFORM)

                        .step("grab the centre box", ctx -> {
                            rememberScale(ctx);
                            press(ctx, center(ctx));
                        })
                        .frames(2)
                        .check("the centre box is being dragged",
                                ctx -> gizmo(ctx).getDragHandle() == TransformGizmo.Handle.UNIFORM)

                        .step("drag out along the screen diagonal", ctx -> drag(ctx, uniformPoint(ctx, DRAG_DISTANCE)))
                        .frames(2)
                        .check("all three axes grew by the same factor", ctx -> {
                            var start = startScale(ctx);
                            var now = scale(ctx);
                            var expected = 1 + DRAG_DISTANCE;
                            ctx.log("scale %s -> %s (x%.2f, x%.2f, x%.2f)".formatted(fmt(start), fmt(now),
                                    now.x / start.x, now.y / start.y, now.z / start.z));
                            return near(now.x, start.x * expected)
                                    && near(now.y / start.y, expected)
                                    && near(now.z / start.z, expected);
                        })

                        .step("drag back to where it was grabbed", ctx -> drag(ctx, center(ctx)))
                        .frames(2)
                        .check("dragging back to the grab restores the scale", ctx -> {
                            var start = startScale(ctx);
                            var now = scale(ctx);
                            return near(now.x, start.x) && near(now.y, start.y) && near(now.z, start.z);
                        })

                        .step("drag inwards, past the centre", ctx -> drag(ctx, uniformPoint(ctx, -2f)))
                        .frames(2)
                        .check("shrinking stops at zero rather than mirroring all three axes", ctx -> {
                            var now = scale(ctx);
                            ctx.log("shrunk to %s".formatted(fmt(now)));
                            return now.x > 0 && now.y > 0 && now.z > 0;
                        })

                        .step("release", ctx -> release(ctx, uniformPoint(ctx, -2f)))
                        .check("the drag ended", ctx -> gizmo(ctx).getDragHandle() == null)
                        .step("put the scale back", ctx -> target(ctx).localScale(new Vector3f(START_SCALE_VALUE))))

                .group("an orthographic camera", g -> g
                        .step("remember how big the gizmo is on screen", ctx ->
                                ctx.put(SCREEN_SIZE, screenSize(ctx)))
                        .step("switch the scene to ortho", ctx -> editor(ctx).scene.useOrtho(true))
                        .frames(10)
                        .check("the gizmo is still the same size on screen", ctx -> {
                            // Both projections put one gizmo unit at the same fraction of the viewport
                            // height, so this is an equality, not an approximation. Before the ortho
                            // camera was taught to answer for itself the gizmo came out ~1/200th of this,
                            // scaled by an eye distance that means nothing without foreshortening.
                            float before = ctx.get(SCREEN_SIZE);
                            var now = screenSize(ctx);
                            ctx.log("gizmo unit: %.1f px in perspective, %.1f px in ortho".formatted(before, now));
                            return Math.abs(now - before) < before * 0.1f;
                        })

                        .step("hover the middle of the gizmo", ctx -> moveTo(ctx, center(ctx)))
                        .frames(HOVER_FRAMES)
                        .check("the centre box is hovered under ortho too",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.UNIFORM)
                        .screenshot("02_scale_gizmo_ortho")

                        .step("grab the centre box", ctx -> {
                            rememberScale(ctx);
                            press(ctx, center(ctx));
                        })
                        .frames(2)
                        .step("drag out along the screen diagonal", ctx -> drag(ctx, uniformPoint(ctx, DRAG_DISTANCE)))
                        .frames(2)
                        .check("the drag scales by what was dragged, not by some fraction of it", ctx -> {
                            var start = startScale(ctx);
                            var now = scale(ctx);
                            var expected = 1 + DRAG_DISTANCE;
                            ctx.log("ortho scale %s -> %s".formatted(fmt(start), fmt(now)));
                            return near(now.x / start.x, expected) && near(now.z / start.z, expected);
                        })
                        .step("release", ctx -> release(ctx, uniformPoint(ctx, DRAG_DISTANCE)))

                        .step("hover the X box", ctx -> moveTo(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH)))
                        .frames(HOVER_FRAMES)
                        // the axis handles are picked against colliders in gizmo space rather than
                        // analytically, which is the half of picking a far-away ray origin can silently kill
                        .check("the axis handles are still reachable under ortho",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.AXIS_X)

                        // The pick ray is anchored on whatever the cursor's pixel hit, so an error in its
                        // direction only shows up at a different depth than that hit. Everything above
                        // aims at a handle drawn over the target itself, which is exactly where the two
                        // agree; this puts the gizmo far off the view axis and aims at a handle standing
                        // in empty sky, where the cursor's pixel is the far plane and the gizmo is not.
                        .step("shrink the target and pan the gizmo well off the view axis", ctx -> {
                            target(ctx).localScale(new Vector3f(0.5f, 0.5f, 0.5f));
                            editor(ctx).scene.setCenter(
                                    new Vector3f(center(ctx)).add(screenRight(ctx).mul(PAN_BLOCKS)));
                        })
                        .frames(10)
                        .step("hover the Y box, which now stands clear of everything",
                                ctx -> moveTo(ctx, axisPoint(ctx, 1, TransformGizmo.AXIS_LENGTH)))
                        .frames(HOVER_FRAMES)
                        .check("a handle over empty space is picked where it is drawn",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.AXIS_Y)
                        .step("re-centre the camera", ctx -> editor(ctx).scene.setCenter(center(ctx)))
                        .frames(10)

                        .step("back to perspective", ctx -> {
                            editor(ctx).scene.useOrtho(false);
                            target(ctx).localScale(new Vector3f(START_SCALE_VALUE));
                        })
                        .frames(10))

                .step("park the cursor off the gizmo", ctx -> moveTo(ctx, uniformPoint(ctx, 2.5f)))
                .frames(HOVER_FRAMES)
                .check("nothing is hovered outside the gizmo", ctx -> gizmo(ctx).getHoverHandle() == null)

                .step("put the cursor back on the centre box for the capture", ctx -> moveTo(ctx, center(ctx)))
                .frames(HOVER_FRAMES)
                .screenshot("01_scale_gizmo")

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

    /** A copy: {@code localScale()} is a read-only view of the live field and moves under a drag. */
    private static Vector3f scale(TestContext ctx) {
        return new Vector3f(target(ctx).localScale());
    }

    private static void rememberScale(TestContext ctx) {
        ctx.put(START_SCALE, scale(ctx));
    }

    private static Vector3f startScale(TestContext ctx) {
        return ctx.get(START_SCALE);
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) < TOLERANCE;
    }

    private static String fmt(Vector3f v) {
        return "%.2f, %.2f, %.2f".formatted(v.x, v.y, v.z);
    }

    // ---- aiming: world points on the handles ----

    /** A point {@code distance} gizmo-units out along one of the three axes. */
    private static Vector3f axisPoint(TestContext ctx, int axis, float distance) {
        return offsetFromCentre(ctx, axisDirection(ctx, axis), distance);
    }

    private static Vector3f axisDirection(TestContext ctx, int axis) {
        var direction = switch (axis) {
            case 0 -> new Vector3f(1, 0, 0);
            case 1 -> new Vector3f(0, 1, 0);
            default -> new Vector3f(0, 0, 1);
        };
        // the scale gizmo always faces the target's own rotation, whatever the space toggle says
        return target(ctx).rotation().transform(direction);
    }

    /** The middle of the square that scales the two axes perpendicular to {@code axis}. */
    private static Vector3f planePoint(TestContext ctx, int axis) {
        var offset = new Vector3f();
        for (int other = 0; other < 3; other++) {
            if (other != axis) offset.add(axisDirection(ctx, other).mul(TransformGizmo.PLANE_CENTRE));
        }
        return new Vector3f(center(ctx)).add(offset.mul(gizmo(ctx).getGizmoScale()));
    }

    /** {@code distance} gizmo-units out from a square, along the diagonal a drag on it is measured along. */
    private static Vector3f planeDragPoint(TestContext ctx, int axis, float distance) {
        var diagonal = new Vector3f();
        for (int other = 0; other < 3; other++) {
            if (other != axis) diagonal.add(axisDirection(ctx, other));
        }
        return new Vector3f(planePoint(ctx, axis))
                .add(diagonal.normalize().mul(distance * gizmo(ctx).getGizmoScale()));
    }

    /**
     * A point {@code distance} gizmo-units along the direction a centre drag is measured in. Taken from
     * the gizmo rather than recomputed: the sign convention is its business, and a test that decided for
     * itself which way was "out" would keep passing if the gizmo flipped.
     */
    private static Vector3f uniformPoint(TestContext ctx, float distance) {
        return offsetFromCentre(ctx, gizmo(ctx).getScreenDiagonal(), distance);
    }

    private static Vector3f screenRight(TestContext ctx) {
        return SceneAiming.screenRight(editor(ctx).scene);
    }

    private static Vector3f offsetFromCentre(TestContext ctx, Vector3f direction, float distance) {
        var offset = new Vector3f(direction).normalize().mul(distance * gizmo(ctx).getGizmoScale());
        return new Vector3f(center(ctx)).add(offset);
    }

    /**
     * How many pixels one gizmo unit covers, measured across the screen plane so that neither endpoint
     * is nearer the camera than the other — otherwise perspective foreshortening, which ortho does not
     * have, would show up in the comparison as if it were a difference in the gizmo's size.
     */
    private static float screenSize(TestContext ctx) {
        return require(ctx, center(ctx)).distance(require(ctx, uniformPoint(ctx, 1f)));
    }

    // ---- driving the cursor at a world point; see SceneAiming for why it is done this way ----

    @Nullable
    private static Vector2f project(TestContext ctx, Vector3f world) {
        return SceneAiming.project(editor(ctx).scene, world);
    }

    private static Vector2f require(TestContext ctx, Vector3f world) {
        return SceneAiming.require(editor(ctx).scene, world);
    }

    private static void moveTo(TestContext ctx, Vector3f world) {
        SceneAiming.moveTo(ctx, editor(ctx).scene, world);
    }

    private static void press(TestContext ctx, Vector3f world) {
        SceneAiming.press(ctx, editor(ctx).scene, world);
    }

    private static void drag(TestContext ctx, Vector3f world) {
        SceneAiming.drag(ctx, editor(ctx).scene, world);
    }

    private static void release(TestContext ctx, Vector3f world) {
        SceneAiming.release(ctx, editor(ctx).scene, world);
    }
}
