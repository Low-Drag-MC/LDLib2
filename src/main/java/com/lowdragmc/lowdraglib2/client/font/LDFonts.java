package com.lowdragmc.lowdraglib2.client.font;

import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client side entry point for measuring and drawing LDLib UI text.
 * <p>
 * Kept out of {@link com.lowdragmc.lowdraglib2.gui.LDLibFonts} so that naming a font from common code never
 * drags {@code Font}, {@code GUIContext} or the glyph atlases onto a dedicated server's classpath. Only call
 * this from client only code: renderers, {@code *ClientSupport} helpers and the like.
 */
public final class LDFonts {

    private LDFonts() {
    }

    /**
     * The font every LDLib UI element draws and measures with.
     * <p>
     * Returns LDLib's smooth signed distance field renderer, which scales cleanly and falls back to the
     * Minecraft unicode font for glyphs a font does not cover, or the vanilla font when the smooth renderer
     * is turned off in the client config. Both report the same advances, so switching between them never
     * shifts layout, carets or selections.
     */
    public static Font font() {
        return LDLibClientConfig.isSmoothFont() ? LDFontManager.INSTANCE.font() : Minecraft.getInstance().font;
    }

    /**
     * Draws text the way {@code GuiGraphics#text} does, but through LDLib's own glyph render state so the
     * distance field atlas can be sampled linearly, and reusing the cached decomposition when possible so the
     * text is not walked again every frame.
     * <p>
     * Prefer this overload when a {@link Component} is at hand: components compare by value, so a caller that
     * rebuilds an equal component every frame still hits the cache.
     */
    public static void drawText(GUIContext context, Font font, Component text, float x, float y,
                                int color, boolean dropShadow) {
        // decomposing the component is a bidi pass of its own, and a component rebuilt every frame cannot
        // reuse the one it memoized, so it is deferred until the cache actually misses
        drawText(context, font, text, text::getVisualOrderText, x, y, color, dropShadow);
    }

    /**
     * Same as {@link #drawText(GUIContext, Font, Component, float, float, int, boolean)}, keyed by the
     * sequence's identity. Only callers that hold onto the sequence between frames benefit from the cache.
     */
    public static void drawText(GUIContext context, Font font, FormattedCharSequence text, float x, float y,
                                int color, boolean dropShadow) {
        drawText(context, font, text, () -> text, x, y, color, dropShadow);
    }

    /**
     * Plain string overload. Strings compare by value, so a caller formatting the same text every frame still
     * hits the cache; one that formats new text every frame (a counter, a timer) will not, which is why the
     * decomposition is only built on a miss.
     */
    public static void drawText(GUIContext context, Font font, String text, float x, float y,
                                int color, boolean dropShadow) {
        drawText(context, font, text, () -> Component.literal(text).getVisualOrderText(),
                x, y, color, dropShadow);
    }

    private static void drawText(GUIContext context, Font font, Object key,
                                 Supplier<FormattedCharSequence> text,
                                 float x, float y, int color, boolean dropShadow) {
        var collecting = LDFontStats.isCollecting();
        var startedAt = collecting ? System.nanoTime() : 0L;
        var smooth = LDLibClientConfig.isSmoothFont() && font == LDFontManager.INSTANCE.font();
        if (!smooth) {
            // Still measured, so cycling fontRenderMode gives a like for like comparison; that is the whole
            // point of the overlay. Only the draw call count is missing here, because vanilla expands its text
            // into glyph states inside the renderer where there is nothing to count from.
            context.graphics.text(font, text.get(), (int) x, (int) y, color, dropShadow);
            if (collecting) {
                LDFontStats.textDraw(System.nanoTime() - startedAt);
            }
            return;
        }
        // glyph lookups happen while the text is prepared, so the bucket has to be selected around that
        var previousBucket = GlyphBucket.current();
        GlyphBucket.set(GlyphBucket.select(context.pose.pose));
        try {
            var sequence = LDLibClientConfig.isTextLayoutCache()
                    ? LDTextLayoutCache.layout(key, text)
                    : text.get();
            var pose = context.pose.copyPose();
            var scissor = context.graphics.peekScissorStack();
            // prepareText is what turns codepoints into positioned quads, and it runs here rather than at
            // render time so the bucket selected above is still the active one
            var runs = new ArrayList<Run>();
            font.prepareText(sequence, x, y, color, dropShadow, false, 0).visit(new Font.GlyphVisitor() {
                @Override
                public void acceptGlyph(TextRenderable.Styled glyph) {
                    collect(runs, glyph);
                }

                @Override
                public void acceptEffect(TextRenderable effect) {
                    collect(runs, effect);
                }
            });
            for (var run : runs) {
                run.submit(context, pose, scissor, collecting);
            }
            if (collecting) {
                LDFontStats.textDraw(System.nanoTime() - startedAt);
            }
        } finally {
            GlyphBucket.set(previousBucket);
        }
    }

    /**
     * Appends a glyph to the run it belongs to, opening a new one when it does not share the pipeline and
     * atlas page of any run so far.
     * <p>
     * Runs are matched in order rather than through a map because a piece of text almost always resolves to a
     * single page, so the list is one entry long and the scan is a single comparison. More than a couple of
     * entries means glyphs are spread over several atlas pages, which the stats overlay is there to surface.
     */
    private static void collect(List<Run> runs, TextRenderable renderable) {
        var pipeline = renderable.guiPipeline();
        var textureView = renderable.textureView();
        for (var run : runs) {
            if (run.pipeline == pipeline && run.textureView == textureView) {
                run.add(renderable);
                return;
            }
        }
        var run = new Run(pipeline, textureView, LDGlyphRenderState.textureSetup(renderable));
        run.add(renderable);
        runs.add(run);
    }

    /**
     * The glyphs of one text draw that share a pipeline and an atlas page, so they can be handed over as a
     * single element and therefore a single draw call.
     */
    private static final class Run {
        private final RenderPipeline pipeline;
        private final GpuTextureView textureView;
        private final TextureSetup textureSetup;
        private final List<TextRenderable> renderables = new ArrayList<>();
        private float left = Float.MAX_VALUE;
        private float top = Float.MAX_VALUE;
        private float right = -Float.MAX_VALUE;
        private float bottom = -Float.MAX_VALUE;

        private Run(RenderPipeline pipeline, GpuTextureView textureView, TextureSetup textureSetup) {
            this.pipeline = pipeline;
            this.textureView = textureView;
            this.textureSetup = textureSetup;
        }

        private void add(TextRenderable renderable) {
            renderables.add(renderable);
            left = Math.min(left, renderable.left());
            top = Math.min(top, renderable.top());
            right = Math.max(right, renderable.right());
            bottom = Math.max(bottom, renderable.bottom());
        }

        private void submit(GUIContext context, Matrix3x2f pose, @Nullable ScreenRectangle scissor,
                            boolean collecting) {
            if (renderables.isEmpty()) {
                return;
            }
            if (collecting) {
                // the GUI renderer batches by pipeline and texture binding, so a run is a draw call
                LDFontStats.batch(pipeline, textureView);
            }
            // The minimal integer rectangle that still contains the run. Not FloatBlitRenderState#getBounds:
            // that derives the size from the unrounded extent (ceil(x1 - x0)) after flooring the origin, so a
            // run starting at a fractional position comes out up to a pixel short on the right and bottom.
            // Bounds decide which layer node the run lands in, and a rectangle that under-reports its overlap
            // can put it in the same node as something it actually covers, where sorting may reorder the two.
            var bounds = new ScreenRectangle(Mth.floor(left), Mth.floor(top),
                    Mth.ceil(right) - Mth.floor(left), Mth.ceil(bottom) - Mth.floor(top))
                    .transformMaxBounds(pose);
            if (scissor != null) {
                // fully clipped text is dropped here rather than meshed and scissored away
                bounds = scissor.intersection(bounds);
            }
            if (bounds == null) {
                return;
            }
            context.addGuiElement(new LDGlyphRenderState(pipeline, textureView, textureSetup, pose,
                    renderables, scissor, bounds));
        }
    }
}
