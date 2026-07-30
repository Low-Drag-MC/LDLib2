package com.lowdragmc.lowdraglib2.client.font.glyph;

/**
 * Everything the text layout needs to know about a glyph, in the 9 pixel design space, without touching the
 * rasterizer.
 * <p>
 * Measuring text walks every codepoint of every string, so this has to stay cheap: rasterizing there would
 * turn a layout pass into thousands of outline traversals.
 */
public record GlyphMetrics(float advance, float boldOffset, float shadowOffset) {
    public static GlyphMetrics of(float advance) {
        return new GlyphMetrics(advance, 1f, 1f);
    }
}
