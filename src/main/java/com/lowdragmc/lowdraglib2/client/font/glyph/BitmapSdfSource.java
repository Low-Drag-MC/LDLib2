package com.lowdragmc.lowdraglib2.client.font.glyph;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * Turns a vanilla bitmap font sheet into signed distance fields.
 * <p>
 * Metrics follow vanilla's {@link BitmapProvider}: the sheet is split into a grid of cells, the whole cell is
 * the glyph quad, and the advance comes from the rightmost non transparent column. Only the alpha channel is
 * used, so a resource pack that ships genuinely multi coloured bitmap glyphs renders as a solid silhouette;
 * every vanilla sheet is white with alpha, so this is not visible in practice.
 */
@OnlyIn(Dist.CLIENT)
public class BitmapSdfSource implements GlyphSource {
    /**
     * Bitmap fonts are only 8 pixels tall, so the mask is enlarged before the distance transform to give the
     * field enough resolution to describe the outline.
     */
    private static final int UPSCALE = 4;
    private static final int PADDING = 2;

    @Nullable
    private NativeImage image;
    private final Int2IntMap cells = new Int2IntOpenHashMap();
    /**
     * Advance per codepoint, computed once at load. Measuring text must never scan pixels.
     */
    private final Int2IntMap advances = new Int2IntOpenHashMap();
    private final int cellWidth;
    private final int cellHeight;
    private final float scale;
    private final int ascent;

    private BitmapSdfSource(NativeImage image, Int2IntMap cells, int cellWidth, int cellHeight, float scale, int ascent) {
        this.image = image;
        this.cells.putAll(cells);
        this.cells.defaultReturnValue(-1);
        this.advances.defaultReturnValue(-1);
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.scale = scale;
        this.ascent = ascent;
        cells.int2IntEntrySet().forEach(entry -> {
            var originX = (entry.getIntValue() & 0xFFFF) * cellWidth;
            var originY = (entry.getIntValue() >>> 16) * cellHeight;
            var inkWidth = actualWidth(image, originX, originY);
            advances.put(entry.getIntKey(), (int) (0.5 + inkWidth * scale) + 1);
        });
    }

    @Nullable
    @Override
    public GlyphMetrics metrics(int codepoint) {
        var advance = advances.get(codepoint);
        return advance < 0 ? null : GlyphMetrics.of(advance);
    }

    @Nullable
    public static BitmapSdfSource load(ResourceManager resourceManager, BitmapProvider.Definition definition) {
        var file = definition.file().withPrefix("textures/");
        try (InputStream stream = resourceManager.open(file)) {
            var image = NativeImage.read(NativeImage.Format.RGBA, stream);
            var grid = definition.codepointGrid();
            var cellWidth = image.getWidth() / grid[0].length;
            var cellHeight = image.getHeight() / grid.length;
            if (cellWidth <= 0 || cellHeight <= 0) {
                image.close();
                LDLib2.LOGGER.error("Bitmap font {} has an empty codepoint grid", file);
                return null;
            }
            var cells = new Int2IntOpenHashMap();
            for (int row = 0; row < grid.length; row++) {
                var line = grid[row];
                for (int column = 0; column < line.length; column++) {
                    var codepoint = line[column];
                    if (codepoint != 0) {
                        cells.putIfAbsent(codepoint, row << 16 | column);
                    }
                }
            }
            return new BitmapSdfSource(image, cells, cellWidth, cellHeight,
                    (float) definition.height() / cellHeight, definition.ascent());
        } catch (IOException e) {
            LDLib2.LOGGER.error("Failed to read bitmap font {}", file, e);
            return null;
        }
    }

    @Nullable
    @Override
    public RawGlyph glyph(int codepoint) {
        var image = this.image;
        if (image == null) return null;
        var cell = cells.get(codepoint);
        if (cell < 0) return null;

        var originX = (cell & 0xFFFF) * cellWidth;
        var originY = (cell >>> 16) * cellHeight;
        var inkWidth = actualWidth(image, originX, originY);
        var advance = advances.get(codepoint);

        var top = 7f - ascent;
        var right = cellWidth * scale;
        var bottom = top + cellHeight * scale;
        if (inkWidth == 0) {
            return RawGlyph.space(advance);
        }

        var mask = new byte[cellWidth * cellHeight];
        for (int y = 0; y < cellHeight; y++) {
            for (int x = 0; x < cellWidth; x++) {
                var alpha = image.getPixelRGBA(originX + x, originY + y) >>> 24;
                mask[y * cellWidth + x] = (byte) (alpha >= 128 ? 0xFF : 0);
            }
        }
        var upscaled = DistanceTransform.upscale(mask, cellWidth, cellHeight, UPSCALE);
        var padding = PADDING * UPSCALE;
        var sdf = DistanceTransform.fromMask(upscaled, cellWidth * UPSCALE, cellHeight * UPSCALE, padding, padding);

        // the padding widens the quad in design space by the same amount on each side
        var designPadding = PADDING * scale;
        return RawGlyph.of(sdf, cellWidth * UPSCALE + padding * 2, cellHeight * UPSCALE + padding * 2,
                -designPadding, top - designPadding, right + designPadding, bottom + designPadding, advance);
    }

    /**
     * A bitmap sheet is pixel art at a fixed resolution, so the only size it can be drawn crisply at is its
     * own. When the requested size does not line one texel up with one device pixel this returns null and the
     * caller falls back to the smoothly scalable distance field version.
     */
    @Nullable
    @Override
    public RawGlyph rasterGlyph(int codepoint, float oversample, boolean exactOnly) {
        var image = this.image;
        if (image == null) return null;
        // A sheet is pixel art at a fixed resolution. It is crisp at its own size and at whole multiples of
        // it, which is what vanilla does at integer GUI scales. In between it can only be scaled, so it is
        // offered anyway when the caller has said it would rather have blocky than soft.
        if (exactOnly && !isWholeMultiple(oversample * scale)) {
            return null;
        }
        var cell = cells.get(codepoint);
        if (cell < 0) return null;

        var originX = (cell & 0xFFFF) * cellWidth;
        var originY = (cell >>> 16) * cellHeight;
        var inkWidth = actualWidth(image, originX, originY);
        var advance = advances.get(codepoint);
        if (inkWidth == 0) {
            return RawGlyph.space(advance);
        }

        var coverage = new byte[cellWidth * cellHeight];
        for (int y = 0; y < cellHeight; y++) {
            for (int x = 0; x < cellWidth; x++) {
                coverage[y * cellWidth + x] = (byte) (image.getPixelRGBA(originX + x, originY + y) >>> 24);
            }
        }
        var top = 7f - ascent;
        return RawGlyph.of(coverage, cellWidth, cellHeight,
                0f, top, cellWidth * scale, top + cellHeight * scale, advance);
    }

    /**
     * @return true when the requested size is the sheet's own resolution or a whole multiple of it
     */
    private static boolean isWholeMultiple(float ratio) {
        var rounded = Math.round(ratio);
        return rounded >= 1 && Math.abs(ratio - rounded) <= 0.01f * rounded;
    }

    /**
     * Number of columns up to and including the rightmost one holding ink, matching
     * {@code BitmapProvider.Definition#getActualGlyphWidth}.
     */
    private int actualWidth(NativeImage image, int originX, int originY) {
        for (int x = cellWidth - 1; x >= 0; x--) {
            for (int y = 0; y < cellHeight; y++) {
                if (image.getLuminanceOrAlpha(originX + x, originY + y) != 0) {
                    return x + 1;
                }
            }
        }
        return 0;
    }

    @Override
    public void close() {
        if (image != null) {
            image.close();
            image = null;
        }
    }
}
