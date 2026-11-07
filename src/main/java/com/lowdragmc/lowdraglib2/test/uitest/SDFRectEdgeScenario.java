package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import com.mojang.blaze3d.platform.NativeImage;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.joml.Vector4f;

/**
 * SDF rects, white on black, read back pixel by pixel.
 *
 * <p>The anti-aliased edge fades out across the outline, so its outer half lands on pixels just outside the shape.
 * The quad used to end exactly at the shape, leaving those pixels undrawn and the edge hard: the pixel just outside
 * each side has to carry some of the fade, and the one past it none.
 *
 * <p>The shader takes each vertex's position within the shape from the vertex itself (the 26.2 GUI draws from a shared
 * buffer at a per-frame base vertex, so {@code gl_VertexID} can't tell the corners apart). A rect rounded at its
 * top-left corner alone shows the corners came out where they belong.
 */
@LDLRegisterClient(name = "sdf_rect_edges", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SDFRectEdgeScenario implements UIScenario {

    private static final String PLAIN = "plain";
    private static final String ROUNDED_TL = "rounded_tl";
    private static final String STROKE = "stroke";

    private static final int SIZE = 40;
    private static final int GUI_SCALE = 2;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("rendering").guiScale(GUI_SCALE);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("sdf rects", ctx -> new ModularUI(UI.of(root(ctx)), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the rects have laid out", ctx -> element(ctx, STROKE).getSizeWidth() > 0)
                .frames(5)
                .step("read the edges", ctx -> {
                    var frame = FrameCapture.grab();
                    try {
                        var plain = element(ctx, PLAIN);
                        int x0 = px(plain.getPositionX()), y0 = px(plain.getPositionY());
                        int x1 = px(plain.getPositionX() + SIZE), y1 = px(plain.getPositionY() + SIZE);
                        int mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
                        ctx.check("the plain rect is filled", grey(frame, mx, my) > 230, "> 230", grey(frame, mx, my));
                        fringe(ctx, frame, "left", x0 - 1, my, x0 - 2, my);
                        fringe(ctx, frame, "right", x1, my, x1 + 1, my);
                        fringe(ctx, frame, "top", mx, y0 - 1, mx, y0 - 2);
                        fringe(ctx, frame, "bottom", mx, y1, mx, y1 + 1);

                        var rounded = element(ctx, ROUNDED_TL);
                        int rx0 = px(rounded.getPositionX()) + GUI_SCALE, ry0 = px(rounded.getPositionY()) + GUI_SCALE;
                        int rx1 = px(rounded.getPositionX() + SIZE) - GUI_SCALE - 1;
                        int ry1 = px(rounded.getPositionY() + SIZE) - GUI_SCALE - 1;
                        ctx.check("the rounded top-left corner is cut away", grey(frame, rx0, ry0) < 8, "< 8", grey(frame, rx0, ry0));
                        ctx.check("the top-right corner is square", grey(frame, rx1, ry0) > 230, "> 230", grey(frame, rx1, ry0));
                        ctx.check("the bottom-right corner is square", grey(frame, rx1, ry1) > 230, "> 230", grey(frame, rx1, ry1));
                        ctx.check("the bottom-left corner is square", grey(frame, rx0, ry1) > 230, "> 230", grey(frame, rx0, ry1));

                        var stroke = element(ctx, STROKE);
                        int sx0 = px(stroke.getPositionX()), sy0 = px(stroke.getPositionY());
                        int smy = sy0 + px(SIZE) / 2;
                        int inStroke = grey(frame, sx0 + GUI_SCALE, smy);
                        int inside = grey(frame, sx0 + px(SIZE) / 2, smy);
                        ctx.check("the stroke is drawn", inStroke > 230, "> 230", inStroke);
                        ctx.check("the stroke's inside is left empty", inside < 8, "< 8", inside);
                    } finally {
                        FrameCapture.closeQuietly(frame);
                    }
                })
                .screenshot("01_rects")
                .closeScreen();
    }

    /**
     * The pixel just outside an edge sits a quarter of a gui unit past it, inside the fade; the one beyond it sits
     * three quarters past, outside the fade.
     */
    private static void fringe(TestContext ctx, NativeImage frame, String side, int nearX, int nearY, int farX, int farY) {
        int near = grey(frame, nearX, nearY);
        int far = grey(frame, farX, farY);
        ctx.check("the pixel just outside the " + side + " edge carries the fade", near > 4 && near < 128, "5..127", near);
        ctx.check("the pixel past it is untouched", far < 4, "< 4", far);
    }

    private static UIElement root(TestContext ctx) {
        var root = new UIElement();
        root.setId("panel")
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                })
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF000000)))
                .addChild(rect(ctx, PLAIN, 20, SDFRectTexture.of(0xFFFFFFFF)))
                .addChild(rect(ctx, ROUNDED_TL, 80, SDFRectTexture.of(0xFFFFFFFF).setRadius(new Vector4f(12, 0, 0, 0))))
                .addChild(rect(ctx, STROKE, 140, SDFRectTexture.of(0x00000000).setStroke(2).setBorderColor(0xFFFFFFFF)));
        return root;
    }

    private static UIElement rect(TestContext ctx, String key, float x, SDFRectTexture texture) {
        var rect = new UIElement();
        rect.setId(key)
                .layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.left(x);
                    layout.top(20);
                    layout.width(SIZE);
                    layout.height(SIZE);
                })
                .style(style -> style.backgroundTexture(texture));
        ctx.put(key, rect);
        return rect;
    }

    private static int px(float gui) {
        return Math.round(gui * GUI_SCALE);
    }

    /** The green channel, white and black alike having all three equal. */
    private static int grey(NativeImage frame, int x, int y) {
        return frame.getPixel(x, y) >> 8 & 0xFF;
    }

    private static UIElement element(TestContext ctx, String key) {
        return ctx.get(key);
    }
}
