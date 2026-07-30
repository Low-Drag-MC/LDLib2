package com.lowdragmc.lowdraglib2.client.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Set;

/**
 * Opt in instrumentation for the text pipeline, used to check whether the smooth font renderer actually made
 * things cheaper rather than just prettier.
 * <p>
 * Everything is measured at {@link com.lowdragmc.lowdraglib2.gui.LDLibFonts}'s draw entry point, which both
 * every rendering mode goes through, so cycling the mode gives a like for like comparison.
 * <p>
 * The interesting number is <em>render types</em>: one render type is one buffer, and one buffer is one draw
 * call at flush time. Vanilla spreads glyphs over 256x256 atlas pages and gets one render type per page, which
 * is the batching problem this whole thing set out to fix.
 * <p>
 * Collection is off unless the overlay is open, and every hot path checks a single static boolean the JIT can
 * fold away.
 */
@OnlyIn(Dist.CLIENT)
public final class LDFontStats {

    /**
     * @param textDraws   calls to the text draw entry point
     * @param renderTypes distinct render types touched, which is the number of draw calls the text will cost
     * @param layoutNanos time spent laying out and emitting text
     * @param cacheHits   layouts served from the cache
     * @param cacheMisses layouts that had to be built, including text that is never cacheable (obfuscated)
     */
    public record Snapshot(int textDraws, int renderTypes, long layoutNanos, int cacheHits, int cacheMisses,
                           int cachedLayouts, int bakedGlyphs, int bakedRasterGlyphs, int sdfPages,
                           int rasterPages) {
        public static final Snapshot EMPTY = new Snapshot(0, 0, 0L, 0, 0, 0, 0, 0, 0, 0);

        public float cacheHitRate() {
            var total = cacheHits + cacheMisses;
            return total == 0 ? 0f : (float) cacheHits / total;
        }

        public float layoutMillis() {
            return layoutNanos / 1_000_000f;
        }
    }

    private static boolean enabled;
    /**
     * Set while the overlay draws itself, so the overlay's own text does not pollute the next frame.
     */
    private static boolean suspended;

    private static int textDraws;
    private static long layoutNanos;
    private static int cacheHits;
    private static int cacheMisses;
    private static final Set<RenderType> RENDER_TYPES = new ObjectOpenHashSet<>();

    private static Snapshot last = Snapshot.EMPTY;

    private LDFontStats() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            last = Snapshot.EMPTY;
            reset();
        }
    }

    public static boolean isCollecting() {
        return enabled && !suspended;
    }

    public static Snapshot last() {
        return last;
    }

    /**
     * Rolls the running counters into a snapshot and starts a new frame.
     */
    public static void endFrame(int cachedLayouts, int bakedGlyphs, int bakedRasterGlyphs,
                                int sdfPages, int rasterPages) {
        last = new Snapshot(textDraws, RENDER_TYPES.size(), layoutNanos, cacheHits, cacheMisses,
                cachedLayouts, bakedGlyphs, bakedRasterGlyphs, sdfPages, rasterPages);
        reset();
    }

    private static void reset() {
        textDraws = 0;
        layoutNanos = 0L;
        cacheHits = 0;
        cacheMisses = 0;
        RENDER_TYPES.clear();
    }

    public static void suspend(boolean value) {
        suspended = value;
    }

    public static void textDraw(long nanos) {
        textDraws++;
        layoutNanos += nanos;
    }

    public static void cacheHit() {
        cacheHits++;
    }

    public static void cacheMiss() {
        cacheMisses++;
    }

    /**
     * Wraps a buffer source so every render type the text touches is recorded.
     * <p>
     * A wrapper per call rather than one shared instance: text drawn from inside another text draw would
     * otherwise redirect the outer one into the inner one's buffer source, and this only ever runs while the
     * overlay is open.
     */
    public static MultiBufferSource counting(MultiBufferSource delegate) {
        return new CountingBufferSource(delegate);
    }

    private record CountingBufferSource(MultiBufferSource delegate) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            RENDER_TYPES.add(renderType);
            return delegate.getBuffer(renderType);
        }
    }
}
