package com.lowdragmc.lowdraglib2.client.font.glyph;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Provides codepoints that only carry an advance and draw nothing, mirroring vanilla's
 * {@link com.mojang.blaze3d.font.SpaceProvider}.
 */
@OnlyIn(Dist.CLIENT)
public class SpaceSource implements GlyphSource {
    private final Int2FloatMap advances;

    public SpaceSource(Map<Integer, Float> advances) {
        this.advances = new Int2FloatOpenHashMap(advances.size());
        advances.forEach((codepoint, advance) -> this.advances.put(codepoint.intValue(), advance.floatValue()));
    }

    @Nullable
    @Override
    public GlyphMetrics metrics(int codepoint) {
        if (!advances.containsKey(codepoint)) {
            return null;
        }
        return GlyphMetrics.of(advances.get(codepoint));
    }

    @Nullable
    @Override
    public RawGlyph glyph(int codepoint) {
        if (!advances.containsKey(codepoint)) {
            return null;
        }
        return RawGlyph.space(advances.get(codepoint));
    }
}
