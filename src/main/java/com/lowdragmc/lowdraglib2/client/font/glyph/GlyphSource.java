package com.lowdragmc.lowdraglib2.client.font.glyph;

import org.jetbrains.annotations.Nullable;

/**
 * One link in a font's fallback chain. Sources are queried in order and the first one that knows a codepoint
 * wins, which is how LDLib guarantees a glyph is never replaced by the missing glyph box as long as the
 * unifont fallback at the end of the chain covers it.
 */
public interface GlyphSource extends AutoCloseable {
    /**
     * Metrics only lookup, used by everything that measures text. Must not rasterize.
     *
     * @return the metrics, or null when this source does not provide the codepoint
     */
    @Nullable
    GlyphMetrics metrics(int codepoint);

    /**
     * @return the signed distance field glyph for the codepoint, or null when this source does not provide it
     */
    @Nullable
    RawGlyph glyph(int codepoint);

    /**
     * The exact size, plain coverage version of a glyph, used at small sizes where a distance field would look
     * soft. Sources backed by pixel art return their native bitmap and ignore the requested size, because that
     * bitmap already is the crisp answer.
     *
     * The plain coverage version of a glyph, for the atlas rasterized at the size the text is drawn at.
     * <p>
     * Outline fonts can render at any size. Pixel art fonts cannot: they only have their own resolution, so at
     * anything other than a whole multiple of it they can either be scaled (blocky, the way vanilla does it) or
     * decline and let the caller use the smoothly scalable distance field.
     *
     * @param oversample device pixels per design space unit
     * @param exactOnly  when true, decline unless one texel lands on exactly one device pixel
     * @return the coverage glyph, or null when this source will not supply the codepoint at this size
     */
    @Nullable
    default RawGlyph rasterGlyph(int codepoint, float oversample, boolean exactOnly) {
        return glyph(codepoint);
    }

    @Override
    default void close() {
    }
}
