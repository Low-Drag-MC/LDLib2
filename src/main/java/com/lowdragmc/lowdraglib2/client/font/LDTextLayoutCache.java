package com.lowdragmc.lowdraglib2.client.font;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Caches the result of walking a piece of text: which glyph goes where, in which style, with which colour.
 * <p>
 * Vanilla redoes that walk every frame for every string on screen: decomposing the sequence, merging styles,
 * and looking a glyph up per codepoint. Since LDLib's UI redraws continuously, the same strings are walked
 * over and over, so the walk is done once and only the vertex writing is repeated.
 * <p>
 * The cached layout is independent of draw colour, drop shadow and position, so one entry serves every way a
 * string is drawn. Obfuscated text is deliberately not cached, since it has to pick fresh random glyphs each
 * frame.
 * <p>
 * The emit path below mirrors {@code Font.StringRenderOutput} exactly, including the dim factor for shadows,
 * the doubled draw for bold, and the z offset applied to the non shadow pass.
 */
@OnlyIn(Dist.CLIENT)
public class LDTextLayoutCache {
    /**
     * Colour value meaning "use the colour the caller passed to draw", since style colours are 24 bit and can
     * never be negative.
     */
    private static final int NO_COLOR = -1;
    private static final int MAX_ENTRIES = 2048;
    /**
     * Matches {@code Font.SHADOW_OFFSET}: the non shadow pass is nudged forward so it wins the depth test.
     */
    private static final Vector3f SHADOW_Z_OFFSET = new Vector3f(0f, 0f, 0.03f);
    private static final float EFFECT_DEPTH = 0.01f;

    /**
     * @param x            pen position relative to the start of the text
     * @param color        style colour, or {@link #NO_COLOR} to use the draw colour
     */
    public record PositionedGlyph(BakedGlyph glyph, float x, boolean bold, boolean italic,
                                  float boldOffset, float shadowOffset, int color) {
    }

    /**
     * An underline or strikethrough quad, positioned relative to the start of the text.
     */
    public record PositionedEffect(float x0, float y0, float x1, float y1, int color) {
    }

    public record Layout(PositionedGlyph[] glyphs, PositionedEffect[] effects, float width) {
    }

    /**
     * The bucket is part of the key because a layout holds the baked glyphs it resolved, and those differ
     * between the distance field atlas and the exact size one.
     */
    private record CacheKey(Object text, int bucket) {
    }

    /**
     * Marker stored for text {@link #build} refused to cache, so it is not walked again next frame only to be
     * refused again. Obfuscated text is redrawn every frame by definition, so without this it would be the one
     * kind of text that pays the full layout cost forever.
     */
    private static final Layout UNCACHEABLE = new Layout(new PositionedGlyph[0], new PositionedEffect[0], 0f);

    private static final Map<CacheKey, Layout> CACHE = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, Layout> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private LDTextLayoutCache() {
    }

    /**
     * Drops every cached layout. Must be called whenever glyphs are rebaked, because the cache holds direct
     * references to {@link BakedGlyph}s whose atlas pages would otherwise be stale.
     */
    public static void clear() {
        CACHE.clear();
    }

    public static int size() {
        return CACHE.size();
    }

    /**
     * Drops the layouts that resolved their glyphs in one bucket, used when that size is evicted.
     */
    public static void clearBucket(int bucket) {
        CACHE.keySet().removeIf(key -> key.bucket() == bucket);
    }

    /**
     * @param key  cache key. Pass the {@link net.minecraft.network.chat.Component} when there is one, since
     *             components compare by value and callers usually rebuild an equal one every frame. Pass the
     *             sequence itself otherwise, which compares by identity and therefore only helps callers that
     *             keep it around.
     * @param text supplies the text to lay out, only called on a miss. Decomposing a component into a
     *             sequence is itself a bidi pass worth skipping when the layout is already cached.
     * @return the layout, or null when this text must not be cached (obfuscated runs)
     */
    @Nullable
    public static Layout layout(Object key, Supplier<FormattedCharSequence> text) {
        var cacheKey = new CacheKey(key, GlyphBucket.current());
        var cached = CACHE.get(cacheKey);
        if (cached != null) {
            if (LDFontStats.isCollecting()) LDFontStats.cacheHit();
            return cached == UNCACHEABLE ? null : cached;
        }
        if (LDFontStats.isCollecting()) LDFontStats.cacheMiss();
        var layout = build(text.get());
        CACHE.put(cacheKey, layout == null ? UNCACHEABLE : layout);
        return layout;
    }

    @Nullable
    private static Layout build(FormattedCharSequence text) {
        var glyphs = new ArrayList<PositionedGlyph>();
        // FormattedCharSink is consumed through a lambda, so the running state lives in this holder
        var state = new Object() {
            float pen = 0f;
            boolean cacheable = true;
            @Nullable
            List<PositionedEffect> effects;
        };

        text.accept((position, style, codePoint) -> {
            if (style.isObfuscated()) {
                state.cacheable = false;
                return false;
            }
            var fontSet = LDFontManager.INSTANCE.apply(style.getFont());
            var info = fontSet.getGlyphInfo(codePoint, false);
            var baked = fontSet.getGlyph(codePoint);
            var bold = style.isBold();
            var textColor = style.getColor();
            var color = textColor == null ? NO_COLOR : textColor.getValue();

            if (!(baked instanceof EmptyGlyph)) {
                glyphs.add(new PositionedGlyph(baked, state.pen, bold, style.isItalic(),
                        info.getBoldOffset(), info.getShadowOffset(), color));
            }

            var advance = info.getAdvance(bold);
            if (style.isStrikethrough()) {
                if (state.effects == null) state.effects = new ArrayList<>();
                state.effects.add(new PositionedEffect(state.pen - 1f, 4.5f, state.pen + advance, 3.5f, color));
            }
            if (style.isUnderlined()) {
                if (state.effects == null) state.effects = new ArrayList<>();
                state.effects.add(new PositionedEffect(state.pen - 1f, 9f, state.pen + advance, 8f, color));
            }
            state.pen += advance;
            return true;
        });

        if (!state.cacheable) {
            return null;
        }
        return new Layout(glyphs.toArray(new PositionedGlyph[0]),
                state.effects == null ? new PositionedEffect[0] : state.effects.toArray(new PositionedEffect[0]),
                state.pen);
    }

    /**
     * Writes a cached layout into the buffer source, following the same two pass shadow handling vanilla uses.
     *
     * @return the pen position after the text, matching {@code Font#drawInBatch}
     */
    public static int draw(Layout layout, float x, float y, int color, boolean dropShadow, Matrix4f pose,
                           MultiBufferSource buffer, Font.DisplayMode mode, int packedLight) {
        color = (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
        var main = new Matrix4f(pose);
        if (dropShadow) {
            emit(layout, x, y, color, true, pose, buffer, mode, packedLight);
            main.translate(SHADOW_Z_OFFSET);
        }
        emit(layout, x, y, color, false, main, buffer, mode, packedLight);
        return (int) (x + layout.width()) + (dropShadow ? 1 : 0);
    }

    private static void emit(Layout layout, float x, float y, int color, boolean dropShadow, Matrix4f pose,
                             MultiBufferSource buffer, Font.DisplayMode mode, int packedLight) {
        var dim = dropShadow ? 0.25f : 1f;
        var a = (color >> 24 & 0xFF) / 255f;
        var r = (color >> 16 & 0xFF) / 255f * dim;
        var g = (color >> 8 & 0xFF) / 255f * dim;
        var b = (color & 0xFF) / 255f * dim;

        for (var positioned : layout.glyphs()) {
            var glyph = positioned.glyph();
            var styleColor = positioned.color();
            var gr = styleColor == NO_COLOR ? r : (styleColor >> 16 & 0xFF) / 255f * dim;
            var gg = styleColor == NO_COLOR ? g : (styleColor >> 8 & 0xFF) / 255f * dim;
            var gb = styleColor == NO_COLOR ? b : (styleColor & 0xFF) / 255f * dim;
            var offset = dropShadow ? positioned.shadowOffset() : 0f;
            var consumer = buffer.getBuffer(glyph.renderType(mode));
            var glyphX = x + positioned.x() + offset;
            var glyphY = y + offset;
            glyph.render(positioned.italic(), glyphX, glyphY, pose, consumer, gr, gg, gb, a, packedLight);
            if (positioned.bold()) {
                glyph.render(positioned.italic(), glyphX + positioned.boldOffset(), glyphY, pose, consumer,
                        gr, gg, gb, a, packedLight);
            }
        }

        if (layout.effects().length > 0) {
            var white = LDFontManager.INSTANCE.apply(Style.DEFAULT_FONT).whiteGlyph();
            var consumer = buffer.getBuffer(white.renderType(mode));
            var shift = dropShadow ? 1f : 0f;
            for (var effect : layout.effects()) {
                var styleColor = effect.color();
                var er = styleColor == NO_COLOR ? r : (styleColor >> 16 & 0xFF) / 255f * dim;
                var eg = styleColor == NO_COLOR ? g : (styleColor >> 8 & 0xFF) / 255f * dim;
                var eb = styleColor == NO_COLOR ? b : (styleColor & 0xFF) / 255f * dim;
                white.renderEffect(new BakedGlyph.Effect(
                        x + shift + effect.x0(), y + shift + effect.y0(),
                        x + shift + effect.x1(), y + shift + effect.y1(),
                        EFFECT_DEPTH, er, eg, eb, a), pose, consumer, packedLight);
            }
        }
    }
}
