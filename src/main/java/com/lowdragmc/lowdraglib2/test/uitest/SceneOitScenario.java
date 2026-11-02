package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
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
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;

/**
 * A scene drawn with the game's improved (order-independent) transparency looks like the classic one where blending
 * order cannot matter: one stained glass face over stone. The scene is drawn both ways and its centre compared.
 */
@LDLRegisterClient(name = "scene_oit", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SceneOitScenario implements UIScenario {

    private static final String SCENE = "scene";
    private static final String CLASSIC = "classic";
    private static final String IMPROVED_TRANSPARENCY = "improved_transparency";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("scene", "rendering").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("improved transparency off", ctx -> {
                    var option = ctx.mc().options.improvedTransparency();
                    ctx.put(IMPROVED_TRANSPARENCY, option.get());
                    option.set(false);
                })
                .frames(2)
                .openModularUI("a stained glass block in front of stone", this::buildUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the scene has laid out", ctx -> scene(ctx).getSizeWidth() > 0)
                .frames(10)
                .step("sample it blended classically", ctx -> {
                    ctx.check("drawn without the OIT phases", !renderer(ctx).isDrawingWithOit());
                    ctx.put(CLASSIC, ScenePixels.centre(scene(ctx)));
                })
                .step("improved transparency on", ctx -> ctx.mc().options.improvedTransparency().set(true))
                .frames(10)
                .screenshot("oit")
                .step("it looks the same through the OIT phases", ctx -> {
                    ctx.check("drawn with the OIT phases", renderer(ctx).isDrawingWithOit());
                    float changed = ScenePixels.changed(ctx.get(CLASSIC), ScenePixels.centre(scene(ctx)));
                    ctx.check("the centre matches the classic one", changed < 0.02f, "< 0.02 of the pixels changed",
                            changed);
                })
                .step("not following the option", ctx -> renderer(ctx).setFollowImprovedTransparency(false))
                .frames(5)
                .step("it is blended classically again",
                        ctx -> ctx.check("drawn without the OIT phases", !renderer(ctx).isDrawingWithOit()))
                .closeScreen()
                .teardown("improved transparency back", ctx -> {
                    Boolean original = ctx.get(IMPROVED_TRANSPARENCY);
                    if (original != null) ctx.mc().options.improvedTransparency().set(original);
                });
    }

    private ModularUI buildUI(TestContext ctx) {
        var world = new TrackedDummyWorld();
        var rendered = new ArrayList<BlockPos>();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                var pos = new BlockPos(x, y, 0);
                world.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                rendered.add(pos);
            }
        }
        var glass = new BlockPos(0, 0, -1);
        world.setBlockAndUpdate(glass, Blocks.STAINED_GLASS.red().defaultBlockState());
        rendered.add(glass);
        var scene = new Scene();
        scene.layout(layout -> layout.width(160).height(160));
        scene.createScene(world).setRenderedCore(rendered);
        ctx.put(SCENE, scene);
        var root = new UIElement().layout(layout -> layout.paddingAll(10)).addChildren(scene)
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF303030)));
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static Scene scene(TestContext ctx) {
        return ctx.get(SCENE);
    }

    private static WorldSceneRenderer renderer(TestContext ctx) {
        return scene(ctx).getRenderer();
    }
}
