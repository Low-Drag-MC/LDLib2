package com.lowdragmc.lowdraglib2.client.font;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.util.Util;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

/**
 * Builds the render types LDLib's glyph atlas pages are drawn with.
 * <p>
 * These only cover the {@code MultiBufferSource} path, which is what {@link net.minecraft.client.gui.Font}
 * uses for text drawn into the world or a scene. GUI text never reaches a {@code RenderType}: 26.1 defers it
 * to {@link net.minecraft.client.renderer.state.gui.GuiElementRenderState}, which asks for a
 * {@link com.mojang.blaze3d.pipeline.RenderPipeline} and a texture binding instead. The pipeline for that path
 * is the {@code guiPipeline} carried on {@link GlyphRenderTypes}.
 * <p>
 * The distance field pages have to be sampled with {@code LINEAR}, which is what reconstructs the field
 * between texels. Vanilla cannot express that for glyphs (its {@code GlyphRenderState} hardcodes
 * {@code NEAREST}), which is the reason LDLib submits its own render state for GUI text rather than going
 * through {@code GuiGraphics#text}.
 */
public final class LDTextRenderTypes {

    private LDTextRenderTypes() {
    }

    private record Key(Identifier atlasPage, boolean linear) {
    }

    private static final Function<Key, RenderType> NORMAL = Util.memoize(
            key -> RenderType.create("ldlib_text", setup(key, RenderPass.NORMAL)));
    private static final Function<Key, RenderType> SEE_THROUGH = Util.memoize(
            key -> RenderType.create("ldlib_text_see_through", setup(key, RenderPass.SEE_THROUGH)));
    private static final Function<Key, RenderType> POLYGON_OFFSET = Util.memoize(
            key -> RenderType.create("ldlib_text_polygon_offset", setup(key, RenderPass.POLYGON_OFFSET)));

    private enum RenderPass {NORMAL, SEE_THROUGH, POLYGON_OFFSET}

    private static RenderSetup setup(Key key, RenderPass pass) {
        var pipeline = switch (pass) {
            case NORMAL -> key.linear() ? LDLibRenderPipelines.SDF_TEXT : LDLibRenderPipelines.RASTER_TEXT;
            case SEE_THROUGH -> key.linear()
                    ? LDLibRenderPipelines.SDF_TEXT_SEE_THROUGH : LDLibRenderPipelines.RASTER_TEXT_SEE_THROUGH;
            case POLYGON_OFFSET -> key.linear()
                    ? LDLibRenderPipelines.SDF_TEXT_POLYGON_OFFSET : LDLibRenderPipelines.RASTER_TEXT_POLYGON_OFFSET;
        };
        var filter = key.linear() ? FilterMode.LINEAR : FilterMode.NEAREST;
        return RenderSetup.builder(pipeline)
                .withTexture("Sampler0", key.atlasPage(),
                        () -> RenderSystem.getSamplerCache().getClampToEdge(filter))
                .useLightmap()
                .createRenderSetup();
    }

    /**
     * @param atlasPage the texture id the page is registered under
     * @param linear    true for distance field pages, false for pages rasterized at their drawn size
     */
    public static GlyphRenderTypes of(Identifier atlasPage, boolean linear) {
        var key = new Key(atlasPage, linear);
        return new GlyphRenderTypes(
                NORMAL.apply(key),
                SEE_THROUGH.apply(key),
                POLYGON_OFFSET.apply(key),
                linear ? LDLibRenderPipelines.SDF_TEXT_GUI : LDLibRenderPipelines.RASTER_TEXT_GUI);
    }
}
