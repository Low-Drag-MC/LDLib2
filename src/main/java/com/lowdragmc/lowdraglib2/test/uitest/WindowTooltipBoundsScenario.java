package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
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
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A tooltip in a UI hosted by its own OS window has to be kept inside <em>that</em> window.
 *
 * <p>Vanilla decides where to flip a tooltip away from an edge, and how wide to wrap its text, from
 * {@code GuiGraphics#guiWidth()}/{@code guiHeight()} — which read {@code Minecraft#getWindow()}
 * directly. A floating window is smaller than the game's, so without a correction a tooltip near its
 * right edge is measured against a boundary that is nowhere near, never flipped, and simply runs off
 * the side.
 *
 * <p>Checked two ways, because one alone is not enough. The seam itself is asserted directly — what
 * those two methods report while a window's surface is being drawn into, and that it goes back
 * afterwards. Then the consequence is asserted in pixels: with a tooltip showing, the far right of
 * the window's own framebuffer must be <em>byte-identical</em> to the frame before it appeared. That
 * comparison needs no knowledge of what a tooltip looks like, and the paired check that the frame
 * changed on the left is what stops it passing when no tooltip was drawn at all.
 */
@LDLRegisterClient(name = "window_tooltip_bounds", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class WindowTooltipBoundsScenario implements UIScenario {

    private static final String WINDOW = "tooltip_window";
    private static final String HOVER_TARGET = "tooltip_hover_target";
    private static final String BASELINE = "tooltip_baseline_frame";

    /** Wide enough to overflow to the right, narrow enough to fit once flipped. Asserted below. */
    private static final Component LINE_ONE =
            Component.literal("A tooltip long enough to reach the window edge");
    private static final Component LINE_TWO =
            Component.literal("and short enough to fit once it is flipped");

    private static final int WINDOW_WIDTH = 640;
    private static final int WINDOW_HEIGHT = 360;
    /**
     * Where the hovered element sits, as a fraction of the window's width. Right of centre so that a
     * tooltip flipped to its left cannot reach the right-hand probe, and left of the probe so that
     * the element's own hover highlight cannot either.
     */
    private static final float TARGET_X = 0.55f;
    /** The band that must not change: the right-hand edge of the window. */
    private static final float PROBE_RIGHT_FROM = 0.92f;
    /** The band that must change: left of the element, where a flipped tooltip lands. */
    private static final float PROBE_LEFT_TO = 0.45f;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("window", "tooltip").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("open a UI in a window of its own", ctx -> {
                    var window = new ModularUIWindow(new ModularUI(UI.of(root(ctx))), "Tooltips");
                    ctx.require("the window opened",
                            window.open(Integer.MIN_VALUE, Integer.MIN_VALUE, WINDOW_WIDTH, WINDOW_HEIGHT, false));
                    ctx.put(WINDOW, window);
                })
                .frames(30)
                .check("the window is open", ctx -> window(ctx).isOpen())
                .waitUntil("its UI has laid out", ctx -> target(ctx).getSizeWidth() > 0)

                .group("guiWidth and guiHeight follow the surface being drawn into", g -> g
                        .step("check the seam", ctx -> {
                            var surface = surface(ctx);
                            var minecraft = ctx.mc();
                            var graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
                            ctx.check("outside a UI pass they are the game window's",
                                    graphics.guiWidth() == minecraft.getWindow().getGuiScaledWidth()
                                            && graphics.guiHeight() == minecraft.getWindow().getGuiScaledHeight());
                            // The window really is a different size, or the check above and the one
                            // below would agree for the wrong reason.
                            ctx.check("the window is not the same size as the game's",
                                    surface.guiScaledWidth() != minecraft.getWindow().getGuiScaledWidth());
                            try (var ignored = UISurface.push(surface)) {
                                ctx.check("inside the window's pass they are the window's",
                                        graphics.guiWidth() == surface.guiScaledWidth()
                                                && graphics.guiHeight() == surface.guiScaledHeight());
                            }
                            ctx.check("and the game window's again once the pass ends",
                                    graphics.guiWidth() == minecraft.getWindow().getGuiScaledWidth());
                        }))

                .group("the tooltip is kept inside the window", g -> g
                        .step("the fixture can actually tell the two outcomes apart", ctx -> {
                            var width = tooltipWidth();
                            var windowWidth = surface(ctx).guiScaledWidth();
                            // Too narrow and an unflipped tooltip would never reach the right probe,
                            // so the test would pass without the fix. Too wide and a flipped one would
                            // reach it anyway, so it could never pass at all.
                            ctx.check("the tooltip is wide enough to overflow (%d of %d)"
                                            .formatted(width, windowWidth),
                                    width > (1 - TARGET_X + (1 - PROBE_RIGHT_FROM)) * windowWidth);
                            ctx.check("and narrow enough to fit once flipped (%d of %d)"
                                            .formatted(width, windowWidth),
                                    width < PROBE_RIGHT_FROM * windowWidth - 8);
                        })
                        .step("capture the window with nothing hovered",
                                ctx -> ctx.put(BASELINE, FrameCapture.grab(surface(ctx).target())))
                        .step("hover the element", ctx -> ctx.input(window(ctx)).moveTo(target(ctx)))
                        .frames(8)
                        .check("the element is hovered",
                                ctx -> UIDebuggerScenario.isWithin(
                                        window(ctx).getModularUI().getLastHoveredElement(), target(ctx)))
                        .step("compare against the baseline", ctx -> {
                            NativeImage baseline = ctx.get(BASELINE);
                            var now = FrameCapture.grab(surface(ctx).target());
                            try {
                                var surface = surface(ctx);
                                var width = surface.guiScaledWidth();
                                var scale = surface.guiScale();
                                ctx.check("a tooltip was drawn to the left of the element",
                                        differsInColumns(baseline, now, scale, 2, PROBE_LEFT_TO * width));
                                ctx.check("and nothing at all changed at the window's right edge",
                                        !differsInColumns(baseline, now, scale,
                                                PROBE_RIGHT_FROM * width, width));
                            } finally {
                                FrameCapture.closeQuietly(now);
                            }
                        })
                        .screenshotSurface("01_tooltip_inside_the_window", WindowTooltipBoundsScenario::surface))

                .check("no window host ever threw while being driven",
                        ctx -> OsWindowManager.totalFailures() == 0)

                .teardown("release the baseline capture",
                        ctx -> FrameCapture.closeQuietly(ctx.get(BASELINE)))
                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                });
    }

    // ------------------------------------------------------------------------------------ fixtures

    private static UIElement root(TestContext ctx) {
        var target = new Button().setText("Hover Me");
        target.setId("hover_me")
                .layout(layout -> {
                    layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE);
                    layout.leftPercent(TARGET_X * 100);
                    layout.topPercent(45);
                    layout.width(52);
                    layout.height(16);
                })
                .style(style -> style.tooltips(LINE_ONE, LINE_TWO));
        ctx.put(HOVER_TARGET, target);

        var root = new UIElement();
        root.setId("panel")
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.alignItems(AlignItems.FLEX_START);
                })
                // Opaque, so the comparison below is against a stable background rather than whatever
                // the clear colour left behind.
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF1B1B22)))
                .addChild(target);
        return root;
    }

    // ------------------------------------------------------------------------------------- helpers

    private static ModularUIWindow window(TestContext ctx) {
        return ctx.get(WINDOW);
    }

    private static UIElement target(TestContext ctx) {
        return ctx.get(HOVER_TARGET);
    }

    private static UISurface surface(TestContext ctx) {
        var surface = window(ctx).surface();
        if (surface == null) {
            throw new IllegalStateException("The window has no surface yet");
        }
        return surface;
    }

    /**
     * The widest line unwrapped, in GUI units — a bound on the fixture, not a prediction.
     *
     * <p>What is actually drawn may be narrower: the same {@code guiWidth()} this scenario is about
     * also decides where NeoForge wraps tooltip text, so with the fix in place these lines wrap and
     * the rendered tooltip is narrower still. That only helps; the bound is here to fail loudly if
     * the text is ever edited down to something that could not overflow in the first place.
     */
    private static int tooltipWidth() {
        var font = LDLibFonts.font();
        return Math.max(font.width(LINE_ONE), font.width(LINE_TWO));
    }

    /**
     * Whether any pixel differs between two captures within a band of GUI-space columns, over the
     * window's full height.
     *
     * <p>Every pixel, not a sampled grid: a tooltip's border is a couple of pixels wide, and a grid
     * coarse enough to be cheap would step straight over it.
     */
    private static boolean differsInColumns(NativeImage a, NativeImage b, double scale,
                                            float guiX0, float guiX1) {
        int left = clamp((int) (guiX0 * scale), a.getWidth());
        int right = clamp((int) (guiX1 * scale), a.getWidth());
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = left; x < right; x++) {
                if (a.getPixelRGBA(x, y) != b.getPixelRGBA(x, y)) return true;
            }
        }
        return false;
    }

    private static int clamp(int value, int limit) {
        return Math.clamp(value, 0, limit);
    }
}
