package com.lowdragmc.lowdraglib2.client.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * LDLib's stand-in for vanilla's {@link net.minecraft.client.renderer.state.gui.GlyphRenderState}.
 * <p>
 * Differs from it in two ways, both forced by how LDLib submits text.
 * <ul>
 *     <li>The sampler comes from the glyph rather than being fixed at {@code NEAREST}, which is what lets a
 *     distance field page be filtered linearly. That is the reason LDLib text does not go through
 *     {@code GuiGraphics#text} at all.</li>
 *     <li>It carries a whole run of glyphs rather than one. Vanilla can afford one state per glyph with null
 *     bounds because it adds them through {@code addGlyphToCurrentLayer}, which skips layer selection; anything
 *     submitted through {@code addGuiElement} is <em>silently dropped</em> when {@link #bounds()} is null, since
 *     {@code GuiRenderState#findAppropriateNode} has no rectangle to place it by. Grouping a run by pipeline and
 *     texture also keeps a screen of text at one draw call, which is the point of the large atlas pages.</li>
 * </ul>
 */
public record LDGlyphRenderState(RenderPipeline pipeline, GpuTextureView textureView, TextureSetup textureSetup,
                                 Matrix3x2fc pose, List<TextRenderable> renderables,
                                 @Nullable ScreenRectangle scissorArea,
                                 @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
    /**
     * Full block and sky light. GUI text is never lit, and vanilla's own glyph render state hardcodes the same
     * value rather than exposing a constant for it.
     */
    private static final int FULL_BRIGHT = 15728880;

    public static TextureSetup textureSetup(TextRenderable renderable) {
        var sampler = renderable instanceof LDTextRenderable sampled
                ? sampled.sampler()
                : RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        return TextureSetup.singleTextureWithLightmap(renderable.textureView(), sampler);
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        // the pose is 2D, and every glyph in the run shares it, so it is lifted once rather than per glyph
        var matrix = new Matrix4f().mul(pose);
        for (var renderable : renderables) {
            renderable.render(matrix, vertexConsumer, FULL_BRIGHT, true);
        }
    }
}
