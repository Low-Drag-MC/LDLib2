package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A cached scene whose blocks change after its first compile was uploaded. The cache used to release its buffer pack
 * with that upload and never make a new one, so every later compile gave up and the scene drew nothing at all.
 * Going S2 -> S1 -> S2 must give back the first picture, through a burst of cancelled compiles and through the
 * incremental (sync) compile too.
 */
@LDLRegisterClient(name = "scene_recompile", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SceneRecompileScenario implements UIScenario {

    private static final String SCENE = "scene";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "scene").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> EditorPaneMaximizeScenario.editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("dock a scene of a 5x5 patch", ctx -> {
                    var view = new View("Scene", Icons.WIDGET_BASIC);
                    var sceneEditor = new SceneEditor();
                    sceneEditor.layout(layout -> {
                        layout.widthPercent(100);
                        layout.heightPercent(100);
                    });
                    sceneEditor.scene.createScene(ctx.requirePlayer().level())
                            .setRenderedCore(patch(ctx, 2))
                            .useCacheBuffer();
                    view.addChildren(sceneEditor);
                    var container = EditorPaneMaximizeScenario.editor(ctx).centerWindow.getLeftTop();
                    container.addView(view);
                    container.selectView(view);
                    ctx.put(SCENE, sceneEditor.scene);
                })
                .frames(60)
                .step("capture the patch", ctx -> ctx.put("patch", capture(ctx, "01_patch")))

                .step("one block, camera kept", ctx -> scene(ctx).setRenderedCore(patch(ctx, 0), null, false))
                .frames(30)
                .step("capture the block", ctx -> {
                    int[] block = capture(ctx, "02_block");
                    ctx.put("block", block);
                    int changed = differing(block, ctx.get("patch"));
                    ctx.check("the recompiled scene shows the single block", changed > block.length / 50,
                            "> " + block.length / 50 + " px", changed + " px");
                })

                .step("a burst of changes, each cancelling the compile before it", ctx -> {
                    scene(ctx).setRenderedCore(patch(ctx, 2), null, false);
                    scene(ctx).setRenderedCore(patch(ctx, 0), null, false);
                    scene(ctx).setRenderedCore(patch(ctx, 2), null, false);
                })
                .frames(30)
                .step("the patch is back", ctx -> {
                    int[] again = capture(ctx, "03_patch_again");
                    int changed = differing(again, ctx.get("patch"));
                    ctx.check("the last change won and the picture is the first one", changed < again.length / 500,
                            "< " + again.length / 500 + " px", changed + " px");
                    ctx.check("nothing is left compiling", !scene(ctx).<WorldSceneRenderer>getRenderer().isCompiling());
                })

                .step("the sync compile, one block", ctx -> {
                    scene(ctx).syncCompile(true);
                    scene(ctx).setRenderedCore(patch(ctx, 0), null, false);
                })
                .frames(30)
                .step("the block is back", ctx -> {
                    int[] synced = capture(ctx, "04_block_sync");
                    int changed = differing(synced, ctx.get("block"));
                    ctx.check("the sync compile gives the same block", changed < synced.length / 500,
                            "< " + synced.length / 500 + " px", changed + " px");
                })
                .closeScreen();
    }

    private static Scene scene(TestContext ctx) {
        return ctx.get(SCENE);
    }

    private static List<BlockPos> patch(TestContext ctx, int radius) {
        var origin = ctx.requirePlayer().getOnPos();
        var blocks = new ArrayList<BlockPos>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                blocks.add(origin.offset(x, 0, z));
            }
        }
        return blocks;
    }

    /** The scene element's pixels, framebuffer-scaled; the whole frame is written for a human to look at. */
    private static int[] capture(TestContext ctx, String name) {
        var image = FrameCapture.grab();
        try {
            FrameCapture.write(image, ctx.outDir().resolve("screenshots").resolve("scene_recompile").resolve(name + ".png"));
            var scene = scene(ctx);
            double scale = ctx.mc().getWindow().getGuiScale();
            int x0 = Math.max(0, (int) (scene.getPositionX() * scale));
            int y0 = Math.max(0, (int) (scene.getPositionY() * scale));
            int x1 = Math.min(image.getWidth(), (int) ((scene.getPositionX() + scene.getSizeWidth()) * scale));
            int y1 = Math.min(image.getHeight(), (int) ((scene.getPositionY() + scene.getSizeHeight()) * scale));
            ctx.require("the scene has an area on screen", x1 - x0 > 16 && y1 - y0 > 16);
            int[] pixels = new int[(x1 - x0) * (y1 - y0)];
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    pixels[(y - y0) * (x1 - x0) + x - x0] = image.getPixel(x, y);
                }
            }
            return pixels;
        } catch (IOException e) {
            throw new IllegalStateException("could not write " + name, e);
        } finally {
            FrameCapture.closeQuietly(image);
        }
    }

    private static int differing(int[] a, int[] b) {
        int count = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int pa = a[i], pb = b[i];
            for (int shift = 0; shift <= 16; shift += 8) {
                if (Math.abs(((pa >> shift) & 0xFF) - ((pb >> shift) & 0xFF)) > 8) {
                    count++;
                    break;
                }
            }
        }
        return count + Math.abs(a.length - b.length);
    }
}
