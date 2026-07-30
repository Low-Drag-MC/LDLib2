package com.lowdragmc.lowdraglib2.gui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import com.lowdragmc.lowdraglib2.client.font.GlyphBucket;
import com.lowdragmc.lowdraglib2.client.font.LDFontManager;
import com.lowdragmc.lowdraglib2.client.font.LDFontStats;
import com.lowdragmc.lowdraglib2.client.font.LDTextLayoutCache;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@UtilityClass
public final class LDLibFonts {
    public static ResourceLocation JETBRAINS_MONO_BOLD = LDLib2.id("jetbrains_mono_bold");

    /**
     * The font every LDLib UI element draws and measures with.
     * <p>
     * Returns LDLib's smooth signed distance field renderer, which scales cleanly and falls back to the
     * Minecraft unicode font for glyphs a font does not cover, or the vanilla font when the smooth renderer
     * is turned off in the client config. Both report the same advances, so switching between them never
     * shifts layout, carets or selections.
     */
    @OnlyIn(Dist.CLIENT)
    public static Font font() {
        return LDLibClientConfig.isSmoothFont() ? LDFontManager.INSTANCE.font() : Minecraft.getInstance().font;
    }

    /**
     * Draws text the way {@link net.minecraft.client.gui.GuiGraphics#drawString} does, but reusing the cached
     * glyph layout when possible so the text is not walked again every frame.
     * <p>
     * Prefer this overload when a {@link Component} is at hand: components compare by value, so a caller that
     * rebuilds an equal component every frame still hits the cache.
     *
     * @return the pen position after the text
     */
    @OnlyIn(Dist.CLIENT)
    public static int drawText(GuiGraphics graphics, Font font, Component text, float x, float y,
                               int color, boolean dropShadow) {
        // decomposing the component is a bidi pass of its own, and a component rebuilt every frame cannot
        // reuse the one it memoized, so it is deferred until the cache actually misses
        return drawText(graphics, font, text, text::getVisualOrderText, x, y, color, dropShadow);
    }

    /**
     * Same as {@link #drawText(GuiGraphics, Font, Component, float, float, int, boolean)}, keyed by the
     * sequence's identity. Only callers that hold onto the sequence between frames benefit from the cache.
     */
    @OnlyIn(Dist.CLIENT)
    public static int drawText(GuiGraphics graphics, Font font, FormattedCharSequence text, float x, float y,
                               int color, boolean dropShadow) {
        return drawText(graphics, font, text, () -> text, x, y, color, dropShadow);
    }

    @OnlyIn(Dist.CLIENT)
    private static int drawText(GuiGraphics graphics, Font font, Object key,
                                Supplier<FormattedCharSequence> text,
                                float x, float y, int color, boolean dropShadow) {
        var collecting = LDFontStats.isCollecting();
        var startedAt = collecting ? System.nanoTime() : 0L;
        var pose = graphics.pose().last().pose();
        // the wrapper records which render types the text touches, which is what the batching win shows up in
        var bufferSource = collecting ? LDFontStats.counting(graphics.bufferSource()) : graphics.bufferSource();
        var smooth = LDLibClientConfig.isSmoothFont() && font == LDFontManager.INSTANCE.font();
        // glyph lookups happen while emitting, so the bucket has to be selected around the whole draw
        var previousBucket = GlyphBucket.current();
        if (smooth) {
            GlyphBucket.set(GlyphBucket.select(pose));
        }
        try {
            var layout = smooth && LDLibClientConfig.isTextLayoutCache()
                    ? LDTextLayoutCache.layout(key, text)
                    : null;
            int result;
            if (layout != null) {
                result = LDTextLayoutCache.draw(layout, x, y, color, dropShadow, pose, bufferSource,
                        Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT);
            } else {
                result = font.drawInBatch(text.get(), x, y, color, dropShadow, pose, bufferSource,
                        Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
            // mirrors GuiGraphics#flushIfUnmanaged, so text still lands in the right layer outside drawManaged
            if (!graphics.managed) {
                graphics.flush();
            }
            if (collecting) {
                LDFontStats.textDraw(System.nanoTime() - startedAt);
            }
            return result;
        } finally {
            GlyphBucket.set(previousBucket);
        }
    }
}
