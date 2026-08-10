package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.mojang.blaze3d.platform.NativeImage;

import java.nio.file.Path;
import java.util.List;

/**
 * A 3D scene rendered inside a floating window.
 *
 * <p>This is the case that broke everything downstream. A scene binds its own framebuffer and then
 * has to give control back — and it used to give it back to {@code Minecraft.getMainRenderTarget()}
 * with the game window's viewport. Inside a floating window that is the wrong destination entirely,
 * so the scene, and everything the UI drew after it, landed in the game's frame instead.
 *
 * <p>Two assertions matter here and neither is visible in an ordinary screenshot:
 * <ul>
 *   <li>the floating window's <em>own</em> framebuffer has real content, which is why this captures
 *       the surface's target directly rather than the frame;</li>
 *   <li>the game's frame is left alone, which the main-window screenshot shows.</li>
 * </ul>
 */
@LDLRegisterClient(name = "floating_scene", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class FloatingSceneScenario implements UIScenario {

    private static final String SCENE_VIEW = "scene_view";
    private static final String SCENE_EDITOR = "scene_editor";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "window", "scene").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)

                .step("dock a scene view in the editor", ctx -> {
                    var view = new View("Scene", Icons.WIDGET_BASIC);
                    var sceneEditor = new SceneEditor();
                    sceneEditor.layout(layout -> {
                        layout.widthPercent(100);
                        layout.heightPercent(100);
                    });
                    var origin = ctx.requirePlayer().getOnPos();
                    sceneEditor.scene
                            .createScene(ctx.requirePlayer().level())
                            .setTickWorld(true)
                            .setRenderedCore(List.of(origin,
                                    origin.offset(1, 0, 0), origin.offset(-1, 0, 0),
                                    origin.offset(0, 0, 1), origin.offset(0, 0, -1)))
                            .useCacheBuffer();
                    view.addChildren(sceneEditor);
                    var container = editor(ctx).centerWindow.getLeftTop();
                    container.addView(view);
                    // Selecting it matters: an unselected tab's content is display:none, so the scene
                    // would never render and every assertion below would pass on the chrome alone.
                    container.selectView(view);
                    ctx.put(SCENE_VIEW, view);
                    ctx.put(SCENE_EDITOR, sceneEditor);
                })
                // The scene compiles its cache over several frames before it draws anything.
                .frames(60)
                .step("record what the docked scene looks like", ctx -> {
                    var image = FrameCapture.grab();
                    try {
                        var out = Path.of(System.getProperty("ldlib2.uitest.out", "build/ldlib2-uitest"),
                                "screenshots", "floating_scene", "01_docked_scene.png");
                        ctx.check("the docked scene drew something", !FrameCapture.write(image, out));
                    } catch (java.io.IOException e) {
                        throw new IllegalStateException("could not write the docked capture", e);
                    } finally {
                        FrameCapture.closeQuietly(image);
                    }
                })

                .group("float the scene", g -> g
                        .step("float the scene view", ctx -> ctx.check("floatView reported success",
                                editor(ctx).getFloatingViews().floatView(ctx.get(SCENE_VIEW))))
                        .frames(40)
                        .check("a native window is open",
                                ctx -> editor(ctx).getFloatingViews().windowOf(ctx.get(SCENE_VIEW)) != null)

                        .step("capture the floating window's own framebuffer", ctx -> {
                            var window = editor(ctx).getFloatingViews().windowOf(ctx.get(SCENE_VIEW));
                            ctx.require("the scene is in a native window", window != null);
                            var surface = window.surface();
                            ctx.require("the window has a surface", surface != null);
                            var image = FrameCapture.grab(surface.target());
                            try {
                                var out = Path.of(System.getProperty("ldlib2.uitest.out", "build/ldlib2-uitest"),
                                        "screenshots", "floating_scene", "02_float_window_contents.png");
                                FrameCapture.write(image, out);
                                ctx.attach("float_window_capture", out.toString());
                                // Deliberately NOT "is the whole image uniform": the window's title
                                // bar and the scene editor's own toolbar draw regardless, so that
                                // question passes even when the 3D scene is missing entirely — which
                                // is exactly how the first version of this test went green on a
                                // window whose viewport was blank.
                                ctx.check("the scene itself rendered in the floating window",
                                        !isRegionUniform(image, ctx.get(SCENE_EDITOR), surface.guiScale()));
                            } catch (java.io.IOException e) {
                                throw new IllegalStateException("could not write the float window capture", e);
                            } finally {
                                FrameCapture.closeQuietly(image);
                            }
                        })
                        // The game's own frame: the editor should look normal, minus the floated tab.
                        .screenshot("03_main_window_after_float"))

                .group("dock it back", g -> g
                        .step("dock the scene back", ctx ->
                                editor(ctx).getFloatingViews().dockBack(ctx.get(SCENE_VIEW)))
                        .frames(20)
                        .check("the native window closed",
                                ctx -> editor(ctx).getFloatingViews().windowOf(ctx.get(SCENE_VIEW)) == null)
                        .screenshot("04_scene_docked_again"))

                .check("no window host ever threw while being driven",
                        ctx -> com.lowdragmc.lowdraglib2.client.window.OsWindowManager.totalFailures() == 0)

                .closeScreen();
    }

    private static com.lowdragmc.lowdraglib2.editor.ui.Editor editor(TestContext ctx) {
        return EditorPaneMaximizeScenario.editor(ctx);
    }

    /**
     * Whether the scene element's own rectangle is a single flat colour.
     *
     * <p>Scanned against the element's bounds rather than the whole image so the surrounding chrome
     * cannot answer for it. Sampled on a grid — this runs on the render thread.
     */
    private static boolean isRegionUniform(NativeImage image, UIElement element, double guiScale) {
        int x0 = Math.max(0, (int) (element.getPositionX() * guiScale));
        int y0 = Math.max(0, (int) (element.getPositionY() * guiScale));
        int x1 = Math.min(image.getWidth(), (int) ((element.getPositionX() + element.getSizeWidth()) * guiScale));
        int y1 = Math.min(image.getHeight(), (int) ((element.getPositionY() + element.getSizeHeight()) * guiScale));
        if (x1 - x0 < 2 || y1 - y0 < 2) return true;
        int first = image.getPixelRGBA(x0, y0);
        int stepX = Math.max(1, (x1 - x0) / 48);
        int stepY = Math.max(1, (y1 - y0) / 48);
        for (int y = y0; y < y1; y += stepY) {
            for (int x = x0; x < x1; x += stepX) {
                if (image.getPixelRGBA(x, y) != first) return false;
            }
        }
        return true;
    }
}
