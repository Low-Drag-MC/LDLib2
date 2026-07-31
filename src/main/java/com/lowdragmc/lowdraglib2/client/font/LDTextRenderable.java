package com.lowdragmc.lowdraglib2.client.font;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.gui.font.TextRenderable;

/**
 * A {@link TextRenderable} that knows how its atlas page must be sampled.
 * <p>
 * Vanilla's {@link net.minecraft.client.renderer.state.gui.GlyphRenderState} binds glyph textures with a
 * hardcoded {@code NEAREST} sampler, which is fine for bitmap sheets but destroys a distance field. LDLib
 * therefore submits {@link LDGlyphRenderState} instead, and that needs to know which sampler the page wants.
 */
public interface LDTextRenderable extends TextRenderable {
    GpuSampler sampler();
}
