package com.lowdragmc.lowdraglib2.client.font;

import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4fc;

/**
 * A glyph baked into one of LDLib's atlas pages.
 * <p>
 * Mirrors vanilla's {@link net.minecraft.client.gui.font.glyphs.BakedSheetGlyph} - same quad, same shadow,
 * bold and italic handling, so text laid out by {@link Font} lands in exactly the same place. The one
 * difference is the UV1 channel, which carries the distance field tuning that 26.1 gives no other way to reach
 * the shader with. See {@link LDLibShaders#SDF_TEXT_FORMAT}.
 */
public class LDBakedGlyph implements BakedGlyph, EffectGlyph {
    private final GlyphInfo info;
    private final GlyphRenderTypes renderTypes;
    private final GpuTextureView textureView;
    /**
     * The page's own sampler. It has to travel with the glyph because the GUI path binds the texture itself,
     * and a distance field page is unusable if it is sampled with {@code NEAREST}.
     */
    private final GpuSampler sampler;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float left;
    private final float right;
    private final float up;
    private final float down;

    public LDBakedGlyph(GlyphInfo info, GlyphRenderTypes renderTypes, GpuTextureView textureView,
                        GpuSampler sampler, float u0, float u1, float v0, float v1,
                        float left, float right, float up, float down) {
        this.info = info;
        this.renderTypes = renderTypes;
        this.textureView = textureView;
        this.sampler = sampler;
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;
        this.left = left;
        this.right = right;
        this.up = up;
        this.down = down;
    }

    @Override
    public GlyphInfo info() {
        return info;
    }

    @Override
    public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style,
                                             float boldOffset, float shadowOffset) {
        return new GlyphInstance(x, y, color, shadowColor, this, style, boldOffset, shadowOffset);
    }

    @Override
    public TextRenderable createEffect(float x0, float y0, float x1, float y1, float depth, int color,
                                       int shadowColor, float shadowOffset) {
        return new EffectInstance(this, x0, y0, x1, y1, depth, color, shadowColor, shadowOffset);
    }

    private static float extraThickness(boolean bold) {
        return bold ? 0.1f : 0f;
    }

    private float shearBottom() {
        return 1f - 0.25f * down;
    }

    private float shearTop() {
        return 1f - 0.25f * up;
    }

    /**
     * Writes the distance field tuning into the vertex's UV1 (overlay) channel as fixed point.
     * <p>
     * Read straight from the config on every vertex rather than hoisted: the values are two field reads, and
     * caching them would mean invalidating the cache when the config changes, for no measurable gain.
     */
    private static void putSdfParams(VertexConsumer consumer) {
        consumer.setUv1(
                Math.round(LDLibClientConfig.sharpness() * LDLibShaders.SDF_PARAM_SCALE),
                Math.round(LDLibClientConfig.weight() * LDLibShaders.SDF_PARAM_SCALE));
    }

    private void renderChar(GlyphInstance instance, Matrix4fc pose, VertexConsumer buffer,
                            int packedLightCoords, boolean flat) {
        var style = instance.style();
        var italic = style.isItalic();
        var x = instance.x();
        var y = instance.y();
        var color = instance.color();
        var bold = style.isBold();
        var zFighter = flat ? 0f : 0.001f;
        float depth;
        if (instance.hasShadow()) {
            var shadowColor = instance.shadowColor();
            render(italic, x + instance.shadowOffset(), y + instance.shadowOffset(), 0f, pose, buffer,
                    shadowColor, bold, packedLightCoords);
            if (bold) {
                render(italic, x + instance.boldOffset() + instance.shadowOffset(), y + instance.shadowOffset(),
                        zFighter, pose, buffer, shadowColor, true, packedLightCoords);
            }
            depth = flat ? 0f : Font.SHADOW_DEPTH;
        } else {
            depth = 0f;
        }
        render(italic, x, y, depth, pose, buffer, color, bold, packedLightCoords);
        if (bold) {
            render(italic, x + instance.boldOffset(), y, depth + zFighter, pose, buffer, color, true,
                    packedLightCoords);
        }
    }

    private void render(boolean italic, float x, float y, float z, Matrix4fc pose, VertexConsumer builder,
                        int color, boolean bold, int packedLightCoords) {
        var x0 = x + left;
        var x1 = x + right;
        var y0 = y + up;
        var y1 = y + down;
        var shearY0 = italic ? shearTop() : 0f;
        var shearY1 = italic ? shearBottom() : 0f;
        var extra = extraThickness(bold);
        vertex(builder, pose, x0 + shearY0 - extra, y0 - extra, z, color, u0, v0, packedLightCoords);
        vertex(builder, pose, x0 + shearY1 - extra, y1 + extra, z, color, u0, v1, packedLightCoords);
        vertex(builder, pose, x1 + shearY1 + extra, y1 + extra, z, color, u1, v1, packedLightCoords);
        vertex(builder, pose, x1 + shearY0 + extra, y0 - extra, z, color, u1, v0, packedLightCoords);
    }

    private static void vertex(VertexConsumer builder, Matrix4fc pose, float x, float y, float z, int color,
                               float u, float v, int packedLightCoords) {
        builder.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(packedLightCoords);
        putSdfParams(builder);
    }

    private void renderEffect(EffectInstance effect, Matrix4fc pose, VertexConsumer buffer,
                              int packedLightCoords, boolean flat) {
        var depth = flat ? 0f : effect.depth();
        if (effect.hasShadow()) {
            buildEffect(effect, effect.shadowOffset(), depth, effect.shadowColor(), buffer, packedLightCoords, pose);
            depth += flat ? 0f : Font.SHADOW_DEPTH;
        }
        buildEffect(effect, 0f, depth, effect.color(), buffer, packedLightCoords, pose);
    }

    private void buildEffect(EffectInstance effect, float offset, float z, int color, VertexConsumer buffer,
                             int packedLightCoords, Matrix4fc pose) {
        vertex(buffer, pose, effect.x0() + offset, effect.y1() + offset, z, color, u0, v0, packedLightCoords);
        vertex(buffer, pose, effect.x1() + offset, effect.y1() + offset, z, color, u0, v1, packedLightCoords);
        vertex(buffer, pose, effect.x1() + offset, effect.y0() + offset, z, color, u1, v1, packedLightCoords);
        vertex(buffer, pose, effect.x0() + offset, effect.y0() + offset, z, color, u1, v0, packedLightCoords);
    }

    private float left(GlyphInstance instance) {
        return instance.x() + left
                + (instance.style().isItalic() ? Math.min(shearTop(), shearBottom()) : 0f)
                - extraThickness(instance.style().isBold());
    }

    private float top(GlyphInstance instance) {
        return instance.y() + up - extraThickness(instance.style().isBold());
    }

    private float right(GlyphInstance instance) {
        return instance.x() + right
                + (instance.hasShadow() ? instance.shadowOffset() : 0f)
                + (instance.style().isItalic() ? Math.max(shearTop(), shearBottom()) : 0f)
                + extraThickness(instance.style().isBold());
    }

    private float bottom(GlyphInstance instance) {
        return instance.y() + down
                + (instance.hasShadow() ? instance.shadowOffset() : 0f)
                + extraThickness(instance.style().isBold());
    }
    private record GlyphInstance(float x, float y, int color, int shadowColor, LDBakedGlyph glyph, Style style,
                                 float boldOffset, float shadowOffset) implements LDTextRenderable, TextRenderable.Styled {
        @Override
        public float left() {
            return glyph.left(this);
        }

        @Override
        public float top() {
            return glyph.top(this);
        }

        @Override
        public float right() {
            return glyph.right(this);
        }

        @Override
        public float bottom() {
            return glyph.bottom(this);
        }

        @Override
        public float activeRight() {
            return x + glyph.info.getAdvance(style.isBold());
        }

        private boolean hasShadow() {
            return shadowColor != 0;
        }

        @Override
        public void render(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, boolean flat) {
            glyph.renderChar(this, pose, buffer, packedLightCoords, flat);
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode, boolean blur) {
            return glyph.renderTypes.select(displayMode);
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return glyph.renderTypes.select(displayMode);
        }

        @Override
        public GpuTextureView textureView() {
            return glyph.textureView;
        }

        @Override
        public RenderPipeline guiPipeline() {
            return glyph.renderTypes.guiPipeline();
        }

        @Override
        public GpuSampler sampler() {
            return glyph.sampler;
        }
    }
    private record EffectInstance(LDBakedGlyph glyph, float x0, float y0, float x1, float y1, float depth,
                                  int color, int shadowColor, float shadowOffset) implements LDTextRenderable {
        @Override
        public float left() {
            return x0;
        }

        @Override
        public float top() {
            return y0;
        }

        @Override
        public float right() {
            return x1 + (hasShadow() ? shadowOffset : 0f);
        }

        @Override
        public float bottom() {
            return y1 + (hasShadow() ? shadowOffset : 0f);
        }

        private boolean hasShadow() {
            return shadowColor != 0;
        }

        @Override
        public void render(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, boolean flat) {
            glyph.renderEffect(this, pose, buffer, packedLightCoords, flat);
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode, boolean blur) {
            return glyph.renderTypes.select(displayMode);
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return glyph.renderTypes.select(displayMode);
        }

        @Override
        public GpuTextureView textureView() {
            return glyph.textureView;
        }

        @Override
        public RenderPipeline guiPipeline() {
            return glyph.renderTypes.guiPipeline();
        }

        @Override
        public GpuSampler sampler() {
            return glyph.sampler;
        }
    }
}
