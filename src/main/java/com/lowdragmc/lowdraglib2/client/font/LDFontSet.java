package com.lowdragmc.lowdraglib2.client.font;

import com.lowdragmc.lowdraglib2.client.font.glyph.DistanceTransform;
import com.lowdragmc.lowdraglib2.client.font.glyph.GlyphMetrics;
import com.lowdragmc.lowdraglib2.client.font.glyph.GlyphSource;
import com.lowdragmc.lowdraglib2.client.font.glyph.RawGlyph;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * A {@link FontSet} whose glyphs come from LDLib's own signed distance field rasterizer instead of vanilla's
 * baked bitmap sheets.
 * <p>
 * Only glyph lookup is replaced. Everything above it, {@link net.minecraft.client.gui.Font}'s
 * {@code StringSplitter}, style handling, bidi, effects and {@code GuiGraphics#drawString}, keeps working
 * unchanged, because advances and quad bounds stay in the same 9 pixel design space vanilla uses.
 * <p>
 * The {@code sources} list is the fallback chain: the first source that knows a codepoint wins. LDLib always
 * appends the unifont provider at the end, so a codepoint only degrades to the missing glyph box if even
 * unifont does not have it.
 */
@OnlyIn(Dist.CLIENT)
public class LDFontSet extends FontSet {
    private static final RandomSource RANDOM = RandomSource.create();
    /**
     * Vanilla's missing glyph is a 5 by 8 hollow rectangle sitting on the whole line height.
     */
    private static final int MISSING_WIDTH = 5;
    private static final int MISSING_HEIGHT = 8;
    private static final int MISSING_UPSCALE = 4;
    private static final int MISSING_PADDING = 2;

    private final LDFontManager manager;
    private final List<GlyphSource> sources;
    private final Int2ObjectMap<GlyphInfo> glyphInfos = new Int2ObjectOpenHashMap<>();
    /**
     * Baked glyphs per {@link GlyphBucket}. Advances never differ between buckets, only the artwork does, so
     * switching bucket can never move text around.
     */
    private final Int2ObjectMap<Int2ObjectMap<BakedGlyph>> bakedGlyphs = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<BakedGlyph> whiteGlyphs = new Int2ObjectOpenHashMap<>();
    /**
     * Codepoints already baked, grouped by rounded advance. Obfuscated text picks replacements from here.
     * Unlike vanilla this only contains glyphs that have actually been drawn, which avoids walking every
     * provider's full codepoint set on load and looks the same on screen.
     */
    private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap<>();
    /**
     * Codepoints already in {@link #glyphsByWidth}, since a codepoint gets baked once per bucket.
     */
    private final IntSet widthRegistered = new IntOpenHashSet();

    @Nullable
    private BakedGlyph missingGlyph;

    public LDFontSet(LDFontManager manager, ResourceLocation name, List<GlyphSource> sources) {
        super(Minecraft.getInstance().getTextureManager(), name);
        this.manager = manager;
        this.sources = sources;
    }

    @Nullable
    private RawGlyph find(int codepoint) {
        for (var source : sources) {
            var glyph = source.glyph(codepoint);
            if (glyph != null) {
                return glyph;
            }
        }
        return null;
    }

    /**
     * @param glyph  the artwork to bake
     * @param raster true when it came from the exact size path and belongs in the nearest filtered atlas
     */
    private record Resolved(RawGlyph glyph, boolean raster) {
    }

    /**
     * Resolves a codepoint for a bucket. In a raster bucket a source that owns the codepoint but cannot map
     * its artwork one texel to one device pixel at this size falls back to its own distance field version, so
     * a line can mix both; that only costs one extra draw call.
     */
    @Nullable
    private Resolved resolve(int codepoint, int bucket) {
        if (bucket == GlyphBucket.SDF) {
            var glyph = find(codepoint);
            return glyph == null ? null : new Resolved(glyph, false);
        }
        var oversample = GlyphBucket.oversample(bucket);
        for (var source : sources) {
            var raster = source.rasterGlyph(codepoint, oversample, GlyphBucket.rasterExactOnly());
            if (raster != null) {
                return new Resolved(raster, true);
            }
            var glyph = source.glyph(codepoint);
            if (glyph != null) {
                return new Resolved(glyph, false);
            }
        }
        return null;
    }

    /**
     * Measuring text runs through here for every codepoint of every string, so it must only ever touch the
     * cheap metrics path. Rasterizing here would put outline tracing on the layout hot path.
     */
    @Override
    public GlyphInfo getGlyphInfo(int character, boolean filterFishyGlyphs) {
        var info = glyphInfos.get(character);
        if (info == null) {
            GlyphMetrics metrics = null;
            for (var source : sources) {
                metrics = source.metrics(character);
                if (metrics != null) break;
            }
            info = metrics == null ? SpecialGlyphs.MISSING : new SdfGlyphInfo(character, metrics);
            glyphInfos.put(character, info);
        }
        if (filterFishyGlyphs) {
            var advance = info.getAdvance();
            if (advance < 0f || advance > 32f) {
                return SpecialGlyphs.MISSING;
            }
        }
        return info;
    }

    @Override
    public BakedGlyph getGlyph(int character) {
        var bucket = GlyphBucket.current();
        var cache = bakedGlyphs.computeIfAbsent(bucket, key -> new Int2ObjectOpenHashMap<>());
        var baked = cache.get(character);
        if (baked == null) {
            var resolved = resolve(character, bucket);
            if (resolved == null) {
                baked = missingGlyph();
            } else if (!resolved.glyph().hasBitmap()) {
                baked = EmptyGlyph.INSTANCE;
            } else {
                var target = resolved.raster() ? bucket : GlyphBucket.SDF;
                baked = bake(resolved.glyph(), target);
                if (baked == null && target != GlyphBucket.SDF) {
                    // the rasterized bitmap is larger than a whole page of its atlas, which the distance field
                    // version cannot be since it is bounded by sdfEmSize
                    var fallback = resolve(character, GlyphBucket.SDF);
                    baked = fallback == null || !fallback.glyph().hasBitmap()
                            ? null
                            : bake(fallback.glyph(), GlyphBucket.SDF);
                }
                if (baked == null) {
                    baked = missingGlyph();
                } else if (!(baked instanceof EmptyGlyph)) {
                    registerWidth(character);
                }
            }
            cache.put(character, baked);
        }
        return baked;
    }

    /**
     * Adds a codepoint to the pool obfuscated text picks replacements from, once. A codepoint is baked again
     * for every bucket it is drawn in, so without this the pool would hold one entry per bucket and weight
     * the random pick towards whatever has been drawn at the most sizes.
     */
    private void registerWidth(int character) {
        if (!widthRegistered.add(character)) {
            return;
        }
        glyphsByWidth.computeIfAbsent(Mth.ceil(getGlyphInfo(character, false).getAdvance()),
                key -> new IntArrayList()).add(character);
    }

    @Override
    public BakedGlyph getRandomGlyph(GlyphInfo glyph) {
        var candidates = glyphsByWidth.get(Mth.ceil(glyph.getAdvance(false)));
        if (candidates == null || candidates.isEmpty()) {
            return missingGlyph();
        }
        return getGlyph(candidates.getInt(RANDOM.nextInt(candidates.size())));
    }

    /**
     * The fully opaque quad used for underline, strikethrough and text background. It lives in the same atlas
     * page as the glyphs, so effects batch together with the text instead of forcing an extra draw call.
     */
    @Override
    public BakedGlyph whiteGlyph() {
        var bucket = GlyphBucket.current();
        var cached = whiteGlyphs.get(bucket);
        if (cached == null) {
            var slot = manager.atlas(bucket).whiteSlot();
            cached = new BakedGlyph(slot.page().renderTypes(),
                    slot.u0(), slot.u1(), slot.v0(), slot.v1(), 0f, 1f, 0f, 1f);
            whiteGlyphs.put(bucket, cached);
        }
        return cached;
    }

    private BakedGlyph missingGlyph() {
        if (missingGlyph == null) {
            var mask = new byte[MISSING_WIDTH * MISSING_HEIGHT];
            for (int y = 0; y < MISSING_HEIGHT; y++) {
                for (int x = 0; x < MISSING_WIDTH; x++) {
                    var border = x == 0 || x + 1 == MISSING_WIDTH || y == 0 || y + 1 == MISSING_HEIGHT;
                    mask[y * MISSING_WIDTH + x] = (byte) (border ? 0xFF : 0);
                }
            }
            var upscaled = DistanceTransform.upscale(mask, MISSING_WIDTH, MISSING_HEIGHT, MISSING_UPSCALE);
            var padding = MISSING_PADDING * MISSING_UPSCALE;
            var sdf = DistanceTransform.fromMask(upscaled, MISSING_WIDTH * MISSING_UPSCALE,
                    MISSING_HEIGHT * MISSING_UPSCALE, padding, padding);
            var baked = bake(RawGlyph.of(sdf,
                    MISSING_WIDTH * MISSING_UPSCALE + padding * 2, MISSING_HEIGHT * MISSING_UPSCALE + padding * 2,
                    -MISSING_PADDING, -MISSING_PADDING,
                    MISSING_WIDTH + MISSING_PADDING, MISSING_HEIGHT + MISSING_PADDING,
                    MISSING_WIDTH + 1), GlyphBucket.SDF);
            // its bitmap is a fixed 44x40, so it only fails to fit if the atlas is unusably small
            missingGlyph = baked == null ? EmptyGlyph.INSTANCE : baked;
        }
        return missingGlyph;
    }

    /**
     * Total glyphs rasterized and uploaded since the last reload, reported by the stats overlay.
     */
    private static int bakedSdfCount;
    private static int bakedRasterCount;

    public static int bakedGlyphCount() {
        return bakedSdfCount + bakedRasterCount;
    }

    /**
     * How many glyphs came from an exact size raster atlas rather than the distance field. If this stays at
     * zero in RASTER mode, the glyphs in question could not be rasterized crisply at the sizes in use and
     * quietly fell back, which is the usual reason raster mode looks no different.
     */
    public static int bakedRasterCount() {
        return bakedRasterCount;
    }

    static void resetBakedGlyphCount() {
        bakedSdfCount = 0;
        bakedRasterCount = 0;
    }

    /**
     * @return the baked glyph, {@link EmptyGlyph} when the artwork has no ink at all, or null when the atlas
     * could not take the bitmap, which is the caller's cue to fall back rather than draw nothing
     */
    @Nullable
    private BakedGlyph bake(RawGlyph raw, int bucket) {
        var data = raw.data();
        if (data == null) {
            return EmptyGlyph.INSTANCE;
        }
        var slot = manager.atlas(bucket).add(raw.width(), raw.height(), data);
        if (slot == null) {
            return null;
        }
        if (bucket == GlyphBucket.SDF) {
            bakedSdfCount++;
        } else {
            bakedRasterCount++;
        }
        return new BakedGlyph(slot.page().renderTypes(), slot.u0(), slot.u1(), slot.v0(), slot.v1(),
                raw.left(), raw.right(), raw.top(), raw.bottom());
    }

    /**
     * Forgets the glyphs baked for one size, called when that size's atlas is evicted.
     */
    public void dropBucket(int bucket) {
        bakedGlyphs.remove(bucket);
        whiteGlyphs.remove(bucket);
    }

    @Override
    public void close() {
        super.close();
        glyphInfos.clear();
        bakedGlyphs.clear();
        whiteGlyphs.clear();
        glyphsByWidth.clear();
        widthRegistered.clear();
        missingGlyph = null;
    }

    /**
     * Adapts a rasterized glyph to the interface vanilla's text pipeline measures with.
     */
    private class SdfGlyphInfo implements GlyphInfo {
        private final int codepoint;
        private final GlyphMetrics metrics;

        private SdfGlyphInfo(int codepoint, GlyphMetrics metrics) {
            this.codepoint = codepoint;
            this.metrics = metrics;
        }

        @Override
        public float getAdvance() {
            return metrics.advance();
        }

        @Override
        public float getBoldOffset() {
            return metrics.boldOffset();
        }

        @Override
        public float getShadowOffset() {
            return metrics.shadowOffset();
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> glyphProvider) {
            // LDLib bakes into its own atlas, so the vanilla stitcher is not used
            return getGlyph(codepoint);
        }
    }
}
