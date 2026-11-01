package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.OffscreenSurface;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import com.mojang.blaze3d.platform.NativeImage;
import dev.vfyjxf.taffy.style.TaffyPosition;

/**
 * An element drawn through a visual layer (opacity below 1, or a mask) in a UI hosted by its own OS window has to
 * land where it would without the layer.
 *
 * <p>The layer renders its subtree into a picture-in-picture texture spanning the screen and blends it back. That
 * screen used to be the game window's, in its gui units, while the subtree is laid out in the window's: the layer
 * came out squeezed into the game window's extent in a window laid out larger than the game - a big one, or one at a
 * smaller gui scale of its own - and stretched in one laid out smaller.
 *
 * <p>Each window holds two translucent squares, near its top-left corner and near its right edge, beside an opaque
 * square that needs no layer and so shows the spot is drawn at all.
 */
@LDLRegisterClient(name = "window_visual_layer", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class WindowVisualLayerScenario implements UIScenario {

    private static final String WINDOW = "visual_layer_window";
    private static final String NEAR = "translucent_near";
    private static final String FAR = "translucent_far";
    private static final String OPAQUE = "opaque_far";

    private static final int SQUARE = 40;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("window", "rendering").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.group("a window laid out larger than the game, at a gui scale of 1",
                        g -> windowCase(g, "01_larger", 1500, 700, 1,
                                ctx -> surface(ctx).guiScaledWidth() > ctx.mc().getWindow().getGuiScaledWidth()))
                .group("a window laid out smaller than the game, at the game's scale",
                        g -> windowCase(g, "02_smaller", 640, 360, OffscreenSurface.FOLLOW_GAME,
                                ctx -> surface(ctx).guiScaledWidth() < ctx.mc().getWindow().getGuiScaledWidth()))
                .check("no window host ever threw while being driven",
                        ctx -> OsWindowManager.totalFailures() == 0)
                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                });
    }

    private static ScenarioBuilder windowCase(ScenarioBuilder g, String label, int width, int height, int guiScale,
                                              java.util.function.Predicate<TestContext> sizedAsMeant) {
        return g.step("open the window", ctx -> {
                    var window = new ModularUIWindow(new ModularUI(UI.of(root(ctx))), "Visual layers");
                    window.setGuiScale(guiScale);
                    ctx.require("the window opened",
                            window.open(Integer.MIN_VALUE, Integer.MIN_VALUE, width, height, false));
                    ctx.put(WINDOW, window);
                })
                .frames(30)
                .waitUntil("its UI has laid out", ctx -> element(ctx, FAR).getSizeWidth() > 0)
                .check("it is laid out at the size the case is about", sizedAsMeant)
                .frames(5)
                .step("compare the squares", ctx -> {
                    var surface = surface(ctx);
                    var frame = FrameCapture.grab(surface.target());
                    try {
                        int near = centreGrey(frame, element(ctx, NEAR), surface.guiScale());
                        int far = centreGrey(frame, element(ctx, FAR), surface.guiScale());
                        int opaque = centreGrey(frame, element(ctx, OPAQUE), surface.guiScale());
                        ctx.check("the opaque square is drawn", opaque > 230, "> 230", opaque);
                        ctx.check("the translucent square near the corner is half white",
                                near > 100 && near < 160, "100..160", near);
                        ctx.check("the translucent square near the right edge is half white",
                                far > 100 && far < 160, "100..160", far);
                    } finally {
                        FrameCapture.closeQuietly(frame);
                    }
                })
                .screenshotSurface(label, WindowVisualLayerScenario::surface)
                .step("close the window", ctx -> OsWindowManager.close(window(ctx)))
                .frames(5);
    }

    private static UIElement root(TestContext ctx) {
        var root = new UIElement();
        root.setId("panel")
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                })
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF000000)))
                .addChild(square(ctx, NEAR, 10, 10, 0, 0.5f))
                .addChild(square(ctx, FAR, 85, 10, 0, 0.5f))
                .addChild(square(ctx, OPAQUE, 85, 10, SQUARE + 10, 1f));
        return root;
    }

    /** At {@code xPercent} of the window's width, so the square is near the same edge whatever the window's size. */
    private static UIElement square(TestContext ctx, String key, float xPercent, float y, float yOffset, float opacity) {
        var square = new UIElement();
        square.setId(key)
                .layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.leftPercent(xPercent);
                    layout.top(y + yOffset);
                    layout.width(SQUARE);
                    layout.height(SQUARE);
                })
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFFFFFFFF)).opacity(opacity));
        ctx.put(key, square);
        return square;
    }

    /** The green channel at the element's centre, in the window's framebuffer. */
    private static int centreGrey(NativeImage frame, UIElement element, double scale) {
        int x = (int) ((element.getPositionX() + element.getSizeWidth() / 2) * scale);
        int y = (int) ((element.getPositionY() + element.getSizeHeight() / 2) * scale);
        return frame.getPixel(x, y) >> 8 & 0xFF;
    }

    private static ModularUIWindow window(TestContext ctx) {
        return ctx.get(WINDOW);
    }

    private static UIElement element(TestContext ctx, String key) {
        return ctx.get(key);
    }

    private static UISurface surface(TestContext ctx) {
        var surface = window(ctx).surface();
        if (surface == null) {
            throw new IllegalStateException("The window has no surface yet");
        }
        return surface;
    }
}
