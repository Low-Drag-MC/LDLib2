package com.lowdragmc.lowdraglib2.client.font;

import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;

/**
 * Decides, for a piece of text about to be drawn, whether its glyphs come from the scalable distance field
 * atlas or from an atlas rasterized at the size it is actually being drawn at.
 * <p>
 * A distance field describes an outline, not pixels. It scales and rotates for free, but it has no hinting, so
 * a one pixel stem landing between two device pixels correctly becomes two grey pixels rather than one solid
 * one. Rasterizing at the exact size lets FreeType's hinting snap that stem onto the grid instead, which is
 * what every desktop UI toolkit does and why their text looks crisper.
 * <p>
 * The catch is that a rasterized size only serves that size. Anything that changes size every frame would
 * re-bake the whole alphabet every frame, so this class deliberately refuses to open new sizes when they are
 * arriving quickly, and such text falls back to the distance field, which is exactly the case the distance
 * field is good at.
 * <p>
 * The selected bucket is read while a piece of text is being <em>prepared</em>, not while its vertices are
 * written: {@code LDFontSet}'s lazy glyph resolves its artwork inside {@code createGlyph}, which runs during
 * {@code Font#prepareText}. {@link com.lowdragmc.lowdraglib2.client.font.LDFonts} therefore selects the bucket
 * around that call rather than around the draw. It is plain static state because all of it happens on the
 * render thread within one text draw.
 */
public final class GlyphBucket {
    /**
     * Bucket value meaning "use the scalable distance field atlas".
     */
    public static final int SDF = 0;
    /**
     * Below this a rasterized glyph has too few pixels to be worth a whole atlas.
     */
    private static final int MIN_RASTER_LINE_HEIGHT = 6;
    /**
     * How far the line height may sit from a whole number of device pixels and still count as an exact match.
     * At an exact match one texel is one device pixel and nearest sampling keeps the glyph perfectly crisp.
     */
    private static final float EXACT_EPSILON = 0.02f;
    private static final float AXIS_EPSILON = 1.0e-4f;
    /**
     * The design space line height every Minecraft font shares.
     */
    public static final float DESIGN_LINE_HEIGHT = 9f;

    /**
     * How many new sizes may be opened before new ones are refused until things settle.
     * <p>
     * This exists to stop a zoom animation from re-baking the alphabet every frame, so it only has to be low
     * enough to catch a runaway. It must stay comfortably above the number of distinct font sizes a normal
     * screen uses, or opening one would push the rest onto the distance field for a second or two and the
     * screen would visibly settle in waves.
     */
    private static final int NEW_BUCKET_BURST = 16;
    private static final long BURST_WINDOW_MS = 500L;

    private static int current = SDF;
    private static final IntSet KNOWN_BUCKETS = new IntOpenHashSet();
    private static final Int2LongMap LAST_USED = new Int2LongOpenHashMap();
    private static int recentlyOpened;
    private static long burstWindowStart;

    private GlyphBucket() {
    }

    /**
     * @return the active bucket, either {@link #SDF} or a device space line height in pixels
     */
    public static int current() {
        return current;
    }

    public static void set(int bucket) {
        current = bucket;
    }

    /**
     * Device pixels per design space unit for a bucket, which is what glyph sources need to rasterize at the
     * right size.
     */
    public static float oversample(int bucket) {
        return bucket / DESIGN_LINE_HEIGHT;
    }

    /**
     * Forgets which sizes have been baked. Call whenever the atlases are thrown away.
     */
    public static void reset() {
        KNOWN_BUCKETS.clear();
        LAST_USED.clear();
        recentlyOpened = 0;
        current = SDF;
    }

    public static void forget(int bucket) {
        KNOWN_BUCKETS.remove(bucket);
        LAST_USED.remove(bucket);
    }

    /**
     * @return when the bucket was last drawn with, in milliseconds
     */
    public static long lastUsed(int bucket) {
        return LAST_USED.get(bucket);
    }

    public static IntSet knownBuckets() {
        return KNOWN_BUCKETS;
    }

    /**
     * @return true when the active mode only takes the raster path where one texel lands on exactly one device
     * pixel. RASTER is willing to scale a rasterized glyph; AUTO is not, which is what keeps it conservative.
     */
    public static boolean rasterExactOnly() {
        return LDLibClientConfig.fontRenderMode() == LDLibClientConfig.FontRenderMode.AUTO;
    }

    /**
     * Picks the bucket for text about to be drawn with the given transform.
     */
    public static int select(Matrix4fc pose) {
        return select(pose.m00(), pose.m01(), pose.m10(), pose.m11());
    }

    /**
     * The GUI carries a 2D pose, so this is the overload every LDLib UI draw goes through. The upper 2x2 block
     * has the same meaning in both, which is all the scale and skew checks look at.
     */
    public static int select(Matrix3x2fc pose) {
        return select(pose.m00(), pose.m01(), pose.m10(), pose.m11());
    }

    private static int select(float m00, float m01, float m10, float m11) {
        var mode = LDLibClientConfig.fontRenderMode();
        if (mode == LDLibClientConfig.FontRenderMode.SDF || mode == LDLibClientConfig.FontRenderMode.VANILLA) {
            return SDF;
        }
        // length of each transformed basis vector, so a rotated transform still reports its real scale
        var scaleX = (float) Math.sqrt(m00 * m00 + m10 * m10);
        var scaleY = (float) Math.sqrt(m01 * m01 + m11 * m11);
        if (scaleX <= 0f || scaleY <= 0f) {
            return SDF;
        }
        var guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();

        if (mode == LDLibClientConfig.FontRenderMode.AUTO) {
            // Rotated, skewed, non uniform and fractionally scaled text stays on the distance field: it scales
            // for free and needs no re-bake, which is exactly what animated text wants. Everything sitting
            // still on the pixel grid takes the sharper rasterized path.
            if (Math.abs(m01) > AXIS_EPSILON || Math.abs(m10) > AXIS_EPSILON) {
                return SDF;
            }
            if (Math.abs(scaleX - scaleY) > AXIS_EPSILON) {
                return SDF; // non uniform
            }
            var lineHeight = DESIGN_LINE_HEIGHT * scaleX * guiScale;
            var rounded = Math.round(lineHeight);
            if (rounded < MIN_RASTER_LINE_HEIGHT || rounded > LDLibClientConfig.rasterMaxSize()
                    || Math.abs(lineHeight - rounded) > EXACT_EPSILON) {
                return SDF;
            }
            // exact texel mapping is still required, which is what sends pixel art fonts at fractional
            // multiples back to the distance field while outline fonts rasterize at any whole size
            return admit(rounded);
        }

        // RASTER never falls back for quality reasons: rotated, skewed and awkwardly scaled text all get a
        // rasterized glyph and are simply filtered, which is what vanilla does too. The only escape is the
        // size cap, and that is an atlas memory guard rather than a judgement about how it looks.
        var lineHeight = DESIGN_LINE_HEIGHT * Math.max(scaleX, scaleY) * guiScale;
        if (lineHeight > LDLibClientConfig.rasterMaxSize()) {
            return SDF;
        }
        return admit(quantize(Math.max(lineHeight, MIN_RASTER_LINE_HEIGHT)));
    }

    /**
     * Sizes are quantized to whole device pixels so nearby scales share one atlas.
     * <p>
     * Whole pixel steps rather than anything coarser, because rasterized atlases are sampled with nearest
     * neighbour: the residual scale has to stay small enough that no texel column is dropped or doubled.
     * Eviction handles the extra sizes that come from the finer steps.
     */
    public static int quantize(float lineHeight) {
        return Math.round(lineHeight);
    }

    /**
     * Lets an already baked size through immediately, but rate limits opening new ones.
     */
    private static int admit(int bucket) {
        var now = System.currentTimeMillis();
        if (KNOWN_BUCKETS.contains(bucket)) {
            LAST_USED.put(bucket, now);
            return bucket;
        }
        if (now - burstWindowStart > BURST_WINDOW_MS) {
            burstWindowStart = now;
            recentlyOpened = 0;
        }
        if (recentlyOpened >= NEW_BUCKET_BURST) {
            // sizes are arriving faster than baking them could pay off, so ride it out on the distance field
            return SDF;
        }
        recentlyOpened++;
        KNOWN_BUCKETS.add(bucket);
        LAST_USED.put(bucket, now);
        return bucket;
    }
}
