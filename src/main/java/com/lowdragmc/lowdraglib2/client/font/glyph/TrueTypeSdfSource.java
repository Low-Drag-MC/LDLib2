package com.lowdragmc.lowdraglib2.client.font.glyph;

import com.lowdragmc.lowdraglib2.LDLib2;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Rasterizes TrueType/OpenType outlines and turns them into signed distance fields.
 * <p>
 * Rasterization goes through FreeType, the same library Minecraft itself uses for TTF fonts. stb_truetype was
 * tried first and had to be abandoned: its rasterizer writes past the end of its own output buffer at small em
 * sizes, which corrupts the native heap and takes the JVM down a few allocations later. FreeType renders these
 * same fonts every time the vanilla font manager loads them.
 * <p>
 * Metrics mirror vanilla's {@link com.mojang.blaze3d.font.TrueTypeGlyphProvider}: the face is scaled so one em
 * maps to {@code emPixels} device pixels and every metric is divided by the oversample factor to land back in
 * the 9 pixel design space, so advances stay identical to what the vanilla renderer produced.
 */
public class TrueTypeSdfSource implements GlyphSource {
    /**
     * Padding around each glyph, also the distance in pixels the field spreads over.
     */
    private static final int PADDING = 6;
    /**
     * Load flags for a metrics only pass, matching what vanilla's TrueType provider uses.
     */
    private static final int LOAD_METRICS = FreeType.FT_LOAD_BITMAP_METRICS_ONLY | FreeType.FT_LOAD_NO_BITMAP;
    /**
     * Rendering for the distance field. Hinting is deliberately off: it distorts the outline to fit whatever
     * pixel grid it is rasterized on, and the field is then sampled at every other size, so that distortion
     * would be baked in and applied to the wrong grid everywhere.
     */
    private static final int LOAD_UNHINTED = FreeType.FT_LOAD_RENDER | FreeType.FT_LOAD_NO_HINTING;
    /**
     * Rendering for the exact size raster path, where hinting is exactly what is wanted: the glyph is drawn at
     * this size and nowhere else, so snapping stems onto the pixel grid is what makes it crisp.
     */
    private static final int LOAD_HINTED = FreeType.FT_LOAD_RENDER;
    /**
     * How close {@code size * oversample} has to be to a whole pixel size for the exact size raster path to be
     * able to map one texel to one device pixel.
     */
    private static final float EXACT_TOLERANCE = 0.01f;

    @Nullable
    private ByteBuffer fontData;
    @Nullable
    private FT_Face face;
    private final float size;
    private final int emPixels;
    private final float oversample;
    private final float shiftX;
    private final float shiftY;
    private final IntSet skip = new IntArraySet();
    private int currentPixelSize = -1;

    private TrueTypeSdfSource(ByteBuffer fontData, FT_Face face, float size, int emPixels,
                              float shiftX, float shiftY, String skip) {
        this.fontData = fontData;
        this.face = face;
        this.size = size;
        this.emPixels = emPixels;
        this.oversample = emPixels / size;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
        skip.codePoints().forEach(this.skip::add);
    }

    /**
     * @param emPixels pixel height one em is rasterized at, higher keeps corners sharper at large font sizes
     */
    @Nullable
    public static TrueTypeSdfSource load(ResourceManager resourceManager, TrueTypeGlyphProviderDefinition definition,
                                         int emPixels) {
        var file = definition.location().withPrefix("font/");
        ByteBuffer data = null;
        FT_Face face = null;
        try (InputStream stream = resourceManager.open(file)) {
            var bytes = stream.readAllBytes();
            // FreeType keeps a pointer into this buffer, so it has to be off heap and outlive the face
            data = MemoryUtil.memAlloc(bytes.length);
            data.put(bytes).flip();
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                try (var stack = MemoryStack.stackPush()) {
                    var pointer = stack.mallocPointer(1);
                    FreeTypeUtil.assertError(
                            FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), data, 0L, pointer),
                            "Initializing font face");
                    face = FT_Face.create(pointer.get());
                }
                FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE),
                        "Find unicode charmap");
            }
            return new TrueTypeSdfSource(data, face, definition.size(), emPixels,
                    definition.shift().x(), definition.shift().y(), definition.skip());
        } catch (IOException | RuntimeException e) {
            if (face != null) {
                synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                    FreeType.FT_Done_Face(face);
                }
            }
            if (data != null) {
                MemoryUtil.memFree(data);
            }
            LDLib2.LOGGER.error("Failed to load font {}", file, e);
            return null;
        }
    }

    @Nullable
    @Override
    public synchronized GlyphMetrics metrics(int codepoint) {
        var index = glyphIndex(codepoint);
        if (index == 0) {
            return null;
        }
        var face = this.face;
        setPixelSize(emPixels);
        if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(face, index, LOAD_METRICS), "Loading glyph metrics")) {
            return null;
        }
        var slot = face.glyph();
        if (slot == null) {
            return null;
        }
        return GlyphMetrics.of(FreeTypeUtil.x(slot.advance()) / oversample);
    }

    @Nullable
    @Override
    public synchronized RawGlyph glyph(int codepoint) {
        var index = glyphIndex(codepoint);
        if (index == 0) {
            return null;
        }
        var rendered = render(index, emPixels, LOAD_UNHINTED);
        if (rendered == null) {
            return null;
        }
        if (rendered.coverage() == null) {
            return RawGlyph.space(rendered.advance() / oversample);
        }

        // fromCoverage, not fromMask: FreeType's grey levels say where the outline runs inside each edge
        // pixel, and throwing that away is what makes large text look jagged
        var sdf = DistanceTransform.fromCoverage(rendered.coverage(), rendered.width(), rendered.height(),
                PADDING, PADDING);
        var designPadding = PADDING / oversample;
        var left = rendered.bearingLeft() / oversample + shiftX - designPadding;
        var top = 7f - rendered.bearingTop() / oversample + shiftY - designPadding;
        var width = rendered.width() + PADDING * 2;
        var height = rendered.height() + PADDING * 2;
        return RawGlyph.of(sdf, width, height, left, top,
                left + width / oversample, top + height / oversample, rendered.advance() / oversample);
    }

    @Nullable
    @Override
    public synchronized RawGlyph rasterGlyph(int codepoint, float oversample, boolean exactOnly) {
        var index = glyphIndex(codepoint);
        if (index == 0) {
            return null;
        }
        var exactPixelSize = size * oversample;
        var pixelSize = Math.round(exactPixelSize);
        if (pixelSize <= 0) {
            return null;
        }
        // Under nearest sampling one texel has to land on one device pixel, or columns get dropped or doubled.
        // The tolerance is relative because the same absolute slip matters far less on a tall glyph.
        if (exactOnly && Math.abs(exactPixelSize - pixelSize) / pixelSize > EXACT_TOLERANCE) {
            return null;
        }
        // the advance stays in the distance field's design space, layout must not shift between buckets
        var metrics = metrics(codepoint);
        if (metrics == null) {
            return null;
        }
        var rendered = render(index, pixelSize, LOAD_HINTED);
        if (rendered == null) {
            return null;
        }
        if (rendered.coverage() == null) {
            return RawGlyph.space(metrics.advance());
        }
        var left = rendered.bearingLeft() / oversample + shiftX;
        var top = 7f - rendered.bearingTop() / oversample + shiftY;
        return RawGlyph.of(rendered.coverage(), rendered.width(), rendered.height(), left, top,
                left + rendered.width() / oversample, top + rendered.height() / oversample, metrics.advance());
    }

    /**
     * @param coverage 8 bit grayscale rows, or null when the glyph has no bitmap at all (a space)
     */
    private record Rendered(@Nullable byte[] coverage, int width, int height,
                            int bearingLeft, int bearingTop, float advance) {
    }

    @Nullable
    private Rendered render(int glyphIndex, int pixelSize, int loadFlags) {
        var face = this.face;
        if (face == null) {
            return null;
        }
        setPixelSize(pixelSize);
        if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(face, glyphIndex, loadFlags), "Rendering glyph")) {
            return null;
        }
        var slot = face.glyph();
        if (slot == null) {
            return null;
        }
        var advance = FreeTypeUtil.x(slot.advance());
        var bitmap = slot.bitmap();
        var width = bitmap.width();
        var height = bitmap.rows();
        if (width <= 0 || height <= 0) {
            return new Rendered(null, 0, 0, 0, 0, advance);
        }
        if (bitmap.pixel_mode() != FreeType.FT_PIXEL_MODE_GRAY) {
            LDLib2.LOGGER.warn("Glyph {} was not rendered as 8-bit grayscale, skipping", glyphIndex);
            return null;
        }
        // rows can be padded, and a negative pitch means the rows are stored bottom up
        var pitch = bitmap.pitch();
        var stride = Math.abs(pitch);
        var buffer = bitmap.buffer(stride * height);
        if (buffer == null) {
            return new Rendered(null, 0, 0, 0, 0, advance);
        }
        var coverage = new byte[width * height];
        for (int row = 0; row < height; row++) {
            var sourceRow = pitch < 0 ? (height - 1 - row) : row;
            buffer.position(sourceRow * stride);
            buffer.get(coverage, row * width, width);
        }
        buffer.position(0);
        return new Rendered(coverage, width, height, slot.bitmap_left(), slot.bitmap_top(), advance);
    }

    private int glyphIndex(int codepoint) {
        var face = this.face;
        if (face == null || skip.contains(codepoint)) {
            return 0;
        }
        return FreeType.FT_Get_Char_Index(face, codepoint);
    }

    private void setPixelSize(int pixelSize) {
        if (currentPixelSize == pixelSize) {
            return;
        }
        FreeTypeUtil.assertError(FreeType.FT_Set_Pixel_Sizes(face, pixelSize, pixelSize), "Setting pixel size");
        currentPixelSize = pixelSize;
    }

    @Override
    public synchronized void close() {
        if (face != null) {
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                FreeTypeUtil.checkError(FreeType.FT_Done_Face(face), "Deleting font face");
            }
            face = null;
        }
        if (fontData != null) {
            MemoryUtil.memFree(fontData);
            fontData = null;
        }
    }
}
