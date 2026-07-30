package com.lowdragmc.lowdraglib2.client.font;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Caches the result of walking a piece of text: which codepoint carries which style, in visual order.
 * <p>
 * Vanilla redoes that walk every frame for every string on screen - decomposing the component, running bidi
 * and merging styles per character. Since LDLib's UI redraws continuously, the same strings are walked over
 * and over, so the walk is done once and replayed afterwards.
 * <p>
 * What is cached is deliberately only the decomposition, not the laid out glyphs. Position, colour and drop
 * shadow are applied by {@link net.minecraft.client.gui.Font#prepareText} on replay, so one entry serves every
 * way a string is drawn, and every layout rule (effects, background, bounds, obfuscation) stays vanilla's
 * rather than being reimplemented here.
 */
public class LDTextLayoutCache {
    private static final int MAX_ENTRIES = 2048;

    /**
     * A decomposed piece of text, replayable as many times as needed.
     * <p>
     * Replaying is a plain array walk, so styles are handed straight back to the sink without re-merging them.
     */
    public record Layout(int[] positions, Style[] styles, int[] codepoints) implements FormattedCharSequence {
        @Override
        public boolean accept(FormattedCharSink sink) {
            for (int i = 0; i < codepoints.length; i++) {
                if (!sink.accept(positions[i], styles[i], codepoints[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final Map<Object, Layout> CACHE = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Object, Layout> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private LDTextLayoutCache() {
    }

    /**
     * Drops every cached layout. Called whenever the font chains are rebuilt, since a cached decomposition was
     * produced by the language and font options in force at the time.
     */
    public static void clear() {
        CACHE.clear();
    }

    public static int size() {
        return CACHE.size();
    }

    /**
     * @param key  cache key. Pass the {@link net.minecraft.network.chat.Component} when there is one, since
     *             components compare by value and callers usually rebuild an equal one every frame. Pass the
     *             sequence itself otherwise, which compares by identity and therefore only helps callers that
     *             keep it around.
     * @param text supplies the text to lay out, only called on a miss. Decomposing a component into a
     *             sequence is itself a bidi pass worth skipping when the layout is already cached.
     */
    public static Layout layout(Object key, Supplier<FormattedCharSequence> text) {
        var cached = CACHE.get(key);
        if (cached != null) {
            if (LDFontStats.isCollecting()) LDFontStats.cacheHit();
            return cached;
        }
        if (LDFontStats.isCollecting()) LDFontStats.cacheMiss();
        var layout = build(text.get());
        CACHE.put(key, layout);
        return layout;
    }

    private static Layout build(FormattedCharSequence text) {
        List<Style> styles = new ArrayList<>();
        var positions = new ArrayList<Integer>();
        var codepoints = new ArrayList<Integer>();
        text.accept((position, style, codePoint) -> {
            positions.add(position);
            styles.add(style);
            codepoints.add(codePoint);
            return true;
        });
        return new Layout(
                positions.stream().mapToInt(Integer::intValue).toArray(),
                styles.toArray(new Style[0]),
                codepoints.stream().mapToInt(Integer::intValue).toArray());
    }
}
