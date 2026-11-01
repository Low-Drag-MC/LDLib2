package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.SceneLighting;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * What faces the camera in a scene faces the scene's camera.
 *
 * <p>Name tags, flames, thrown items and the like turn by {@code CameraRenderState.orientation}, which the
 * game fills from its camera. A scene hands the submit phase a camera state of its own, so it has to fill
 * that in from the scene camera, or all of them face one fixed way whatever the view. Here a snowball is
 * looked at along the X axis: turned the fixed way it is seen edge-on, and nothing shows.
 */
@LDLRegisterClient(name = "scene_camera_facing", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SceneCameraFacingScenario implements UIScenario {

    private static final String SCENE = "scene";
    private static final String FACING = "facing";
    private static final String LEVEL_LIT = "level_lit";
    private static final int BACKGROUND = 0xFF303030;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("scene", "rendering").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a scene with a snowball", SceneCameraFacingScenario::buildUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the scene has laid out", ctx -> scene(ctx).getSizeWidth() > 0)
                .step("look along the X axis", ctx -> scene(ctx).setCameraYawAndPitch(0, 10))
                .frames(10)
                .screenshot("01_snowball_side_on")
                .step("the submit phase faces the way the scene looks", ctx -> {
                    var facing = ctx.<float[]>get(FACING);
                    ctx.require("the scene submitted a frame", facing != null);
                    WorldSceneRenderer renderer = scene(ctx).getRenderer();
                    var view = new Vector3f(renderer.getLookAt()).sub(renderer.getEyePos()).normalize();
                    var forward = new Vector3f(facing[0], facing[1], facing[2]);
                    float dot = forward.dot(view);
                    ctx.check("the camera state's forward is the view direction", dot > 0.99f,
                            "> 0.99", dot + " (forward " + forward + ", view " + view + ")");
                })
                .step("the snowball faces the camera", ctx -> {
                    var pixels = ScenePixels.centre(scene(ctx));
                    ctx.put(LEVEL_LIT, pixels);
                    float drawn = ScenePixels.notColour(pixels, BACKGROUND);
                    ctx.check("something is drawn at the snowball", drawn > 0.1f, "> 0.1 of the pixels", drawn);
                })
                .step("light the scene as an item in a GUI", ctx -> scene(ctx).setLighting(SceneLighting.ITEMS_3D))
                .frames(5)
                .step("the snowball is shaded differently", ctx -> {
                    float changed = ScenePixels.changed(ctx.get(LEVEL_LIT), ScenePixels.centre(scene(ctx)));
                    ctx.check("the lighting setting reached the renderer", changed > 0.05f, "> 0.05 of the pixels", changed);
                })
                .closeScreen();
    }

    private static ModularUI buildUI(TestContext ctx) {
        var world = new TrackedDummyWorld();
        var snowball = EntityTypes.SNOWBALL.create(world, EntitySpawnReason.COMMAND);
        if (snowball != null) {
            // hangs where the scene looks: at the centre of the one (empty) block it is framed on
            snowball.setPos(0.5, 0.5, 0.5);
            snowball.setNoGravity(true);
            snowball.setDeltaMovement(Vec3.ZERO);
            world.addEntity(snowball);
        }
        var scene = new Scene();
        scene.layout(layout -> layout.width(160).height(160));
        scene.createScene(world).setRenderedCore(List.of(BlockPos.ZERO));
        WorldSceneRenderer renderer = scene.getRenderer();
        renderer.setAfterBuiltinSubmit(c -> {
            // the direction the camera state looks in: Camera's FORWARDS (0, 0, -1) turned by its orientation
            var forward = c.cameraState().orientation.transform(new Vector3f(0, 0, -1));
            ctx.put(FACING, new float[]{forward.x, forward.y, forward.z});
        });
        ctx.put(SCENE, scene);
        var root = new UIElement().layout(layout -> layout.paddingAll(10)).addChildren(scene)
                .style(style -> style.backgroundTexture(new ColorRectTexture(BACKGROUND)));
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static Scene scene(TestContext ctx) {
        return ctx.get(SCENE);
    }
}
