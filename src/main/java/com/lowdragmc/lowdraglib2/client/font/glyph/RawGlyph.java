package com.lowdragmc.lowdraglib2.client.font.glyph;

import org.jetbrains.annotations.Nullable;

/**
 * A rasterized glyph, ready to be uploaded into a glyph atlas.
 * <p>
 * The plane bounds use the same coordinate space as vanilla's {@link com.mojang.blaze3d.font.SheetGlyphInfo}:
 * relative to the pen position at the top of the line, with the baseline sitting at y = 7 in the 9 pixel
 * design space every Minecraft font shares. Keeping this convention means advances and glyph positions stay
 * identical to the vanilla renderer, so measuring code (caret positions, line wrapping) needs no changes.
 *
 * @param data         single channel bitmap, {@code width * height} bytes row major, or null for a blank glyph
 * @param width        bitmap width in atlas pixels
 * @param height       bitmap height in atlas pixels
 * @param left         left edge of the quad in design space
 * @param top          top edge of the quad in design space
 * @param right        right edge of the quad in design space
 * @param bottom       bottom edge of the quad in design space
 * @param advance      pen advance in design space
 * @param boldOffset   how far the glyph is redrawn to fake bold
 * @param shadowOffset how far the drop shadow is offset
 */
public record RawGlyph(@Nullable byte[] data, int width, int height,
                       float left, float top, float right, float bottom,
                       float advance, float boldOffset, float shadowOffset) {

    public static RawGlyph of(byte[] data, int width, int height,
                              float left, float top, float right, float bottom, float advance) {
        return new RawGlyph(data, width, height, left, top, right, bottom, advance, 1f, 1f);
    }

    /**
     * A glyph with no bitmap, only an advance (used by space providers and by whitespace characters).
     */
    public static RawGlyph space(float advance) {
        return new RawGlyph(null, 0, 0, 0f, 0f, 0f, 0f, advance, 1f, 1f);
    }

    public RawGlyph withOffsets(float boldOffset, float shadowOffset) {
        return new RawGlyph(data, width, height, left, top, right, bottom, advance, boldOffset, shadowOffset);
    }

    public boolean hasBitmap() {
        return data != null && width > 0 && height > 0;
    }
}
