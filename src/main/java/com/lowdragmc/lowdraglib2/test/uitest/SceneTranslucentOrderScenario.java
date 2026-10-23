package com.lowdragmc.lowdraglib2.test.uitest;

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
 * A block entity behind glass in a scene must show through it.
 *
 * <p>Translucent terrain writes depth, so it has to be drawn after everything solid, as the game draws its
 * own level. Drawn first, it hides whatever the feature phases put behind it: here a chest inside a shell of
 * stained glass. The centre of the scene is compared with the shell alone, which it must not match, and with
 * the chest alone, which it must not match either, so the glass is still drawn over the chest.
 */
@LDLRegisterClient(name = "scene_translucent_order", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SceneTranslucentOrderScenario implements UIScenario {

    private static final String SCENE = "scene";
    private static final String WORLD = "world";
    private static final String GLASS_ONLY = "glass_only";
    private static final String CHEST_IN_GLASS = "chest_in_glass";
    private static final BlockPos CENTER = BlockPos.ZERO;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("scene", "rendering").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a scene of a stained glass shell", SceneTranslucentOrderScenario::buildUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the scene has laid out", ctx -> scene(ctx).getSizeWidth() > 0)
                .frames(10)
                .step("sample the empty shell", ctx -> ctx.put(GLASS_ONLY, ScenePixels.centre(scene(ctx))))
                .step("put a chest inside", ctx -> {
                    TrackedDummyWorld world = ctx.get(WORLD);
                    world.setBlockAndUpdate(CENTER, Blocks.CHEST.defaultBlockState());
                    scene(ctx).needCompileCache();
                })
                .frames(10)
                .screenshot("01_chest_in_glass")
                .step("the chest shows through the glass", ctx -> {
                    int[] withChest = ScenePixels.centre(scene(ctx));
                    ctx.put(CHEST_IN_GLASS, withChest);
                    checkDiffers(ctx, "the centre changes once the chest is in", ctx.get(GLASS_ONLY), withChest);
                })
                .step("take the glass away", ctx -> {
                    TrackedDummyWorld world = ctx.get(WORLD);
                    for (var pos : BlockPos.betweenClosed(-1, -1, -1, 1, 1, 1)) {
                        if (!pos.equals(CENTER)) world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                    scene(ctx).needCompileCache();
                })
                .frames(10)
                .step("the glass was in front of the chest", ctx -> checkDiffers(ctx,
                        "the bare chest looks different from the chest behind glass", ctx.get(CHEST_IN_GLASS), ScenePixels.centre(scene(ctx))))
                .closeScreen();
    }

    /** Hidden, the chest leaves the frame exactly as it was, so a quarter of the pixels changing is plenty. */
    private static void checkDiffers(TestContext ctx, String description, int[] a, int[] b) {
        float changed = ScenePixels.changed(a, b);
        ctx.check(description, changed > 0.25f, "> 0.25 of the pixels changed", changed);
    }

    private static ModularUI buildUI(TestContext ctx) {
        var world = new TrackedDummyWorld();
        var shell = new ArrayList<BlockPos>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    var pos = new BlockPos(x, y, z);
                    shell.add(pos);
                    if (!pos.equals(CENTER)) {
                        world.setBlockAndUpdate(pos, Blocks.STAINED_GLASS.red().defaultBlockState());
                    }
                }
            }
        }
        var scene = new Scene();
        scene.layout(layout -> layout.width(160).height(160));
        scene.createScene(world).setRenderedCore(shell);
        ctx.put(SCENE, scene);
        ctx.put(WORLD, world);
        // opaque, so nothing the game draws behind the screen can change what the glass is seen against
        var root = new UIElement().layout(layout -> layout.paddingAll(10)).addChildren(scene)
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF303030)));
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static Scene scene(TestContext ctx) {
        return ctx.get(SCENE);
    }
}
