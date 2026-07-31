package com.lowdragmc.lowdraglib2.client.font.glyph;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.LDLib2;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * Reads a GNU Unifont style hex archive and converts the 1 bit bitmaps into signed distance fields.
 * <p>
 * This is the tail of every LDLib fallback chain: unifont covers the whole basic multilingual plane, so once
 * it is in the chain a codepoint can only fall through to the missing glyph box if it is outside the BMP.
 * That is the whole point of it, and it is why LDLib text no longer shows tofu boxes for CJK, Cyrillic, Greek
 * and friends when a custom font does not include them.
 * <p>
 * Rows are stored left aligned in a 32 bit int, exactly like vanilla's {@code UnihexProvider.LineData}, so
 * the ink bounds and advances come out identical.
 */
public class UnihexSdfSource implements GlyphSource {
    private static final int GLYPH_HEIGHT = 16;
    /**
     * Unifont glyphs are already 16 pixels tall, so a x2 enlargement is enough to describe the outline.
     */
    private static final int UPSCALE = 2;
    private static final int PADDING = 2;
    /**
     * Unifont renders at two pixels per design pixel, matching vanilla.
     */
    private static final float OVERSAMPLE = 2f;

    /**
     * @param rows  16 rows of bits, left aligned in a 32 bit int
     * @param left  index of the first ink column, 0 being bit 31
     * @param right index of the last ink column
     */
    private record Entry(int[] rows, int left, int right) {
        int width() {
            return right - left + 1;
        }
    }

    /**
     * A {@code size_overrides} entry, forcing the ink bounds for a codepoint range.
     */
    public record SizeOverride(int from, int to, int left, int right) {
    }

    private final Int2ObjectMap<Entry> entries;

    private UnihexSdfSource(Int2ObjectMap<Entry> entries) {
        this.entries = entries;
    }

    @Nullable
    public static UnihexSdfSource load(ResourceManager resourceManager, Identifier hexFile,
                                       List<SizeOverride> overrides) {
        var rowsByCodepoint = new Int2ObjectOpenHashMap<int[]>();
        try (InputStream stream = resourceManager.open(hexFile);
             ZipInputStream zip = new ZipInputStream(stream)) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                if (entry.getName().endsWith(".hex")) {
                    readHex(new BufferedInputStream(zip), rowsByCodepoint);
                }
                entry = zip.getNextEntry();
            }
        } catch (IOException | IllegalArgumentException e) {
            LDLib2.LOGGER.error("Failed to read unihex font {}", hexFile, e);
            return null;
        }

        var entries = new Int2ObjectOpenHashMap<Entry>(rowsByCodepoint.size());
        for (var override : overrides) {
            for (int codepoint = override.from(); codepoint <= override.to(); codepoint++) {
                var rows = rowsByCodepoint.remove(codepoint);
                if (rows != null) {
                    entries.put(codepoint, new Entry(rows, override.left(), override.right()));
                }
            }
        }
        rowsByCodepoint.int2ObjectEntrySet().forEach(e -> {
            var rows = e.getValue();
            var mask = 0;
            for (int row = 0; row < GLYPH_HEIGHT; row++) {
                mask |= rows[row];
            }
            int left;
            int right;
            if (mask == 0) {
                left = 0;
                right = rows[GLYPH_HEIGHT];
            } else {
                left = Integer.numberOfLeadingZeros(mask);
                right = 32 - Integer.numberOfTrailingZeros(mask) - 1;
            }
            entries.put(e.getIntKey(), new Entry(rows, left, right));
        });
        return new UnihexSdfSource(entries);
    }

    /**
     * Parses the {@code size_overrides} array of a unihex provider entry. The vanilla record hides its fields,
     * so the raw json is read directly instead of going through the codec.
     */
    public static List<SizeOverride> parseOverrides(JsonObject provider) {
        var result = new ArrayList<SizeOverride>();
        var array = provider.getAsJsonArray("size_overrides");
        if (array == null) return result;
        for (var element : array) {
            var object = element.getAsJsonObject();
            var from = object.get("from").getAsString().codePointAt(0);
            var to = object.get("to").getAsString().codePointAt(0);
            result.add(new SizeOverride(from, to, object.get("left").getAsInt(), object.get("right").getAsInt()));
        }
        return result;
    }

    @Nullable
    @Override
    public GlyphMetrics metrics(int codepoint) {
        var entry = entries.get(codepoint);
        if (entry == null) return null;
        return new GlyphMetrics(entry.width() / 2 + 1, 0.5f, 0.5f);
    }

    @Nullable
    @Override
    public RawGlyph glyph(int codepoint) {
        var entry = entries.get(codepoint);
        if (entry == null) return null;

        var width = entry.width();
        var advance = width / 2 + 1;
        var mask = unpack(entry, width);
        if (mask == null) {
            return RawGlyph.space(advance).withOffsets(0.5f, 0.5f);
        }

        var upscaled = DistanceTransform.upscale(mask, width, GLYPH_HEIGHT, UPSCALE);
        var padding = PADDING * UPSCALE;
        var sdf = DistanceTransform.fromMask(upscaled, width * UPSCALE, GLYPH_HEIGHT * UPSCALE, padding, padding);

        var designPadding = PADDING / OVERSAMPLE;
        var left = -designPadding;
        // vanilla uses the default bearing of 7, which puts unifont glyphs at y 0 to 8 in design space
        var top = -designPadding;
        return RawGlyph.of(sdf, width * UPSCALE + padding * 2, GLYPH_HEIGHT * UPSCALE + padding * 2,
                        left, top, width / OVERSAMPLE + designPadding, GLYPH_HEIGHT / OVERSAMPLE + designPadding, advance)
                .withOffsets(0.5f, 0.5f);
    }

    /**
     * Unifont is a 1 bit bitmap at a fixed 2 texels per design pixel, so it can only be drawn crisply at that
     * exact ratio. Anywhere else the distance field version looks better than the row dropping vanilla does.
     */
    @Nullable
    @Override
    public RawGlyph rasterGlyph(int codepoint, float oversample, boolean exactOnly) {
        // unifont is 1 bit pixel art at two texels per design pixel, so only whole multiples of that are crisp
        var ratio = oversample / OVERSAMPLE;
        var rounded = Math.round(ratio);
        if (exactOnly && (rounded < 1 || Math.abs(ratio - rounded) > 0.01f * rounded)) {
            return null;
        }
        var entry = entries.get(codepoint);
        if (entry == null) return null;

        var width = entry.width();
        var advance = width / 2 + 1;
        var mask = unpack(entry, width);
        if (mask == null) {
            return RawGlyph.space(advance).withOffsets(0.5f, 0.5f);
        }
        return RawGlyph.of(mask, width, GLYPH_HEIGHT,
                        0f, 0f, width / OVERSAMPLE, GLYPH_HEIGHT / OVERSAMPLE, advance)
                .withOffsets(0.5f, 0.5f);
    }

    /**
     * @return the coverage mask, or null when the glyph has no ink at all
     */
    @Nullable
    private static byte[] unpack(Entry entry, int width) {
        var mask = new byte[width * GLYPH_HEIGHT];
        var hasInk = false;
        for (int row = 0; row < GLYPH_HEIGHT; row++) {
            var bits = entry.rows()[row];
            for (int column = 0; column < width; column++) {
                var shift = 31 - (entry.left() + column);
                var set = shift >= 0 && shift < 32 && ((bits >>> shift) & 1) != 0;
                if (set) hasInk = true;
                mask[row * width + column] = (byte) (set ? 0xFF : 0);
            }
        }
        return hasInk ? mask : null;
    }

    /**
     * Reads {@code CODEPOINT:BITS} lines. BITS is 32, 64, 96 or 128 hex digits describing 16 rows of an
     * 8, 16, 24 or 32 pixel wide bitmap.
     */
    private static void readHex(InputStream stream, Int2ObjectOpenHashMap<int[]> output) throws IOException {
        var buffer = new StringBuilder(160);
        while (true) {
            buffer.setLength(0);
            if (!readUntil(stream, buffer, ':')) {
                return;
            }
            var length = buffer.length();
            if (length < 4 || length > 6) {
                throw new IllegalArgumentException("Expected 4 to 6 hex digits before the colon, got " + length);
            }
            var codepoint = Integer.parseInt(buffer, 0, length, 16);

            buffer.setLength(0);
            readUntil(stream, buffer, '\n');
            var digits = buffer.length();
            if (digits != 32 && digits != 64 && digits != 96 && digits != 128) {
                throw new IllegalArgumentException("Expected 32, 64, 96 or 128 hex digits for codepoint "
                        + Integer.toHexString(codepoint) + ", got " + digits);
            }
            var digitsPerRow = digits / GLYPH_HEIGHT;
            var bitWidth = digitsPerRow * 4;
            // one extra slot carries the declared bit width, needed to reproduce vanilla's blank glyph width
            var rows = new int[GLYPH_HEIGHT + 1];
            for (int row = 0; row < GLYPH_HEIGHT; row++) {
                var from = row * digitsPerRow;
                var value = Integer.parseUnsignedInt(buffer, from, from + digitsPerRow, 16);
                rows[row] = value << (32 - bitWidth);
            }
            rows[GLYPH_HEIGHT] = bitWidth;
            output.put(codepoint, rows);
        }
    }

    /**
     * @return false when the stream ended before the terminator was found and nothing was read
     */
    private static boolean readUntil(InputStream stream, StringBuilder out, char terminator) throws IOException {
        while (true) {
            var read = stream.read();
            if (read == -1) {
                return false;
            }
            if (read == terminator) {
                return true;
            }
            if (read != '\r') {
                out.append((char) read);
            }
        }
    }
}
