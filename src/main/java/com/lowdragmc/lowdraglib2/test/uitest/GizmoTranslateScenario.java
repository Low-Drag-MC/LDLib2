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
import org.joml.Vector3f;

import java.util.List;

/**
 * The scene editor's move gizmo: three arrows, three squares that slide in an axis plane, and the centre
 * box that slides in the plane of the screen. Aiming works as it does in the other gizmo scenarios — a
 * point on the handle is computed in world space and projected, see {@link SceneAiming}.
 *
 * <p>What this protects:
 * <ul>
 *   <li><b>The four kinds of handle stay separable.</b> The centre box has to beat three axis colliders
 *       that all reach back to the origin, and the squares have to be far enough out not to be a coin
 *       toss against it. Both are pick-order questions, which are the ones that regress quietly: a wrong
 *       answer still moves the target, just not along what was grabbed.</li>
 *   <li><b>A slide keeps the grabbed point under the cursor.</b> Every planar drag — the three squares
 *       and the free one — is that same operation, so the target must land exactly where the cursor went
 *       rather than merely somewhere in the right direction.</li>
 *   <li><b>A plane drag leaves its own axis alone</b>, which is the whole difference between it and the
 *       free one.</li>
 * </ul>
 */
@LDLRegisterClient(name = "gizmo_translate", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class GizmoTranslateScenario implements UIScenario {

    private static final String EDITOR = "gizmo_editor";
    private static final String START_POSITION = "gizmo_start_position";

    /** As in the other gizmo scenarios: an angle where none of the three axes is anywhere near edge-on. */
    private static final float CAMERA_YAW = 35;
    private static final float CAMERA_PITCH = 30;

    /** How far to drag, in gizmo units — a world distance of this many times the gizmo's screen scale. */
    private static final float DRAG_DISTANCE = 0.5f;
    /** Tolerance on a drag, in gizmo units, so it does not depend on how big the gizmo happens to be. */
    private static final float TOLERANCE = 0.1f;

    /**
     * The scene renders {@code lastHit} <em>after</em> it has already called back into the gizmo, so a
     * cursor move is one frame away from changing what is hovered — and a click is answered with the ray
     * from the last rendered frame, so a press has to be given the same grace as a hover check.
     */
    private static final int HOVER_FRAMES = 3;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(40).tags("editor", "scene", "gizmo").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("gizmo_translate_scene", ctx -> {
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
                    sceneEditor.setTransformGizmoMode(TransformGizmo.Mode.TRANSLATE);
                    // GLOBAL keeps the handles on the world axes, so a point computed for the X arrow is
                    // still on the X arrow after an earlier step moved the target
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
                    scene.setCenter(position(ctx));
                })
                .frames(20)
                .waitUntil("the gizmo projects onto the screen", ctx -> project(ctx, position(ctx)) != null)
                .check("the gizmo is active", ctx -> gizmo(ctx).isActive())

                .group("an axis arrow", g -> g
                        .step("hover the X arrow", ctx -> moveTo(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH)))
                        .frames(HOVER_FRAMES)
                        .check("the X arrow is hovered",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.AXIS_X)

                        .step("press on the X arrow", ctx -> {
                            rememberPosition(ctx);
                            press(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH));
                        })
                        .frames(2)
                        .check("the X arrow is being dragged",
                                ctx -> gizmo(ctx).getDragHandle() == TransformGizmo.Handle.AXIS_X)
                        .step("drag it out along X", ctx ->
                                drag(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH + DRAG_DISTANCE)))
                        .frames(2)
                        .step("release", ctx ->
                                release(ctx, axisPoint(ctx, 0, TransformGizmo.AXIS_LENGTH + DRAG_DISTANCE)))
                        .check("the drag ended", ctx -> gizmo(ctx).getDragHandle() == null)
                        .check("the target moved along X only, by what was dragged", ctx ->
                                movedBy(ctx, new Vector3f(DRAG_DISTANCE, 0, 0)))
                        .step("put the target back", ctx -> restorePosition(ctx)))

                .group("a planar square", g -> g
                        .step("hover the square in the YZ plane", ctx -> moveTo(ctx, planePoint(ctx, 0)))
                        .frames(HOVER_FRAMES)
                        .check("the X planar handle is hovered — it is far enough out to aim at",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.PLANE_X)

                        .step("press on it", ctx -> {
                            rememberPosition(ctx);
                            press(ctx, planePoint(ctx, 0));
                        })
                        .frames(2)
                        .check("the X planar handle is being dragged",
                                ctx -> gizmo(ctx).getDragHandle() == TransformGizmo.Handle.PLANE_X)
                        // an asymmetric offset inside the plane: equal amounts would pass on a gizmo that
                        // slid along the square's diagonal instead of following the cursor
                        .step("drag it across its own plane", ctx ->
                                drag(ctx, planeDragPoint(ctx)))
                        .frames(2)
                        .step("release", ctx -> release(ctx, planeDragPoint(ctx)))
                        .check("the target followed the cursor in the plane, and X did not move", ctx ->
                                movedBy(ctx, new Vector3f(0, 0.4f, 0.25f)))
                        .step("put the target back", ctx -> restorePosition(ctx)))

                .group("the centre box", g -> g
                        .step("hover the middle of the gizmo", ctx -> moveTo(ctx, position(ctx)))
                        .frames(HOVER_FRAMES)
                        .check("the middle is the free handle, not an axis one",
                                ctx -> gizmo(ctx).getHoverHandle() == TransformGizmo.Handle.FREE)

                        .step("grab the centre box", ctx -> {
                            rememberPosition(ctx);
                            press(ctx, position(ctx));
                        })
                        .frames(2)
                        .check("the centre box is being dragged",
                                ctx -> gizmo(ctx).getDragHandle() == TransformGizmo.Handle.FREE)

                        .step("drag it across the screen", ctx -> drag(ctx, freeDragPoint(ctx)))
                        .frames(2)
                        .check("the target went where the cursor went, in the plane of the screen", ctx -> {
                            var moved = new Vector3f(position(ctx)).sub(startPosition(ctx));
                            var expected = new Vector3f(gizmo(ctx).getScreenDiagonal()).mul(
                                    DRAG_DISTANCE * gizmo(ctx).getGizmoScale());
                            ctx.log("moved %s, expected %s".formatted(fmt(moved), fmt(expected)));
                            return moved.distance(expected) < TOLERANCE * gizmo(ctx).getGizmoScale();
                        })
                        .step("drag back to where it was grabbed", ctx -> drag(ctx, startPosition(ctx)))
                        .frames(2)
                        .check("dragging back to the grab restores the position", ctx ->
                                movedBy(ctx, new Vector3f()))
                        .step("release", ctx -> release(ctx, position(ctx)))
                        .check("the drag ended", ctx -> gizmo(ctx).getDragHandle() == null))

                .step("park the cursor off the gizmo", ctx -> moveTo(ctx, axisPoint(ctx, 1, 2.5f)))
                .frames(HOVER_FRAMES)
                .check("nothing is hovered outside the gizmo", ctx -> gizmo(ctx).getHoverHandle() == null)

                .step("put the cursor back on the centre box for the capture", ctx -> moveTo(ctx, position(ctx)))
                .frames(HOVER_FRAMES)
                .screenshot("01_translate_gizmo")

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

    private static Vector3f position(TestContext ctx) {
        return target(ctx).position();
    }

    private static void rememberPosition(TestContext ctx) {
        ctx.put(START_POSITION, position(ctx));
    }

    private static Vector3f startPosition(TestContext ctx) {
        return ctx.get(START_POSITION);
    }

    private static void restorePosition(TestContext ctx) {
        target(ctx).position(new Vector3f(startPosition(ctx)));
    }

    /** Whether the target moved by {@code expected}, which is given in gizmo units. */
    private static boolean movedBy(TestContext ctx, Vector3f expected) {
        var scale = gizmo(ctx).getGizmoScale();
        var moved = new Vector3f(position(ctx)).sub(startPosition(ctx));
        var want = new Vector3f(expected).mul(scale);
        ctx.log("moved %s, expected %s".formatted(fmt(moved), fmt(want)));
        return moved.distance(want) < TOLERANCE * scale;
    }

    private static String fmt(Vector3f v) {
        return "%.2f, %.2f, %.2f".formatted(v.x, v.y, v.z);
    }

    // ---- aiming: world points on the handles ----

    private static Vector3f axisPoint(TestContext ctx, int axis, float distance) {
        return offsetFromCentre(ctx, unit(axis).mul(distance));
    }

    /** The middle of the square that slides in the plane perpendicular to {@code axis}. */
    private static Vector3f planePoint(TestContext ctx, int axis) {
        var offset = new Vector3f();
        for (int other = 0; other < 3; other++) {
            if (other != axis) offset.add(unit(other).mul(TransformGizmo.PLANE_CENTRE));
        }
        return offsetFromCentre(ctx, offset);
    }

    /** Where the YZ square is dragged to: different amounts in Y and Z, and nothing along X. */
    private static Vector3f planeDragPoint(TestContext ctx) {
        return new Vector3f(planePoint(ctx, 0))
                .add(new Vector3f(0, 0.4f, 0.25f).mul(gizmo(ctx).getGizmoScale()));
    }

    private static Vector3f freeDragPoint(TestContext ctx) {
        return offsetFromCentre(ctx, new Vector3f(gizmo(ctx).getScreenDiagonal()).mul(DRAG_DISTANCE));
    }

    /** A world offset, given in gizmo units, from the target. */
    private static Vector3f offsetFromCentre(TestContext ctx, Vector3f offsetInGizmoUnits) {
        return new Vector3f(position(ctx)).add(offsetInGizmoUnits.mul(gizmo(ctx).getGizmoScale()));
    }

    private static Vector3f unit(int axis) {
        return switch (axis) {
            case 0 -> new Vector3f(1, 0, 0);
            case 1 -> new Vector3f(0, 1, 0);
            default -> new Vector3f(0, 0, 1);
        };
    }

    // ---- driving the cursor at a world point; see SceneAiming for why it is done this way ----

    private static org.joml.Vector2f project(TestContext ctx, Vector3f world) {
        return SceneAiming.project(editor(ctx).scene, world);
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
