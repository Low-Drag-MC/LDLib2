package com.lowdragmc.lowdraglib2.client.shader;

import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.OptionalDouble;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class LDLibRenderTypes extends RenderType {
    private static final RenderType POSITION_COLOR_NO_DEPTH = create("position_color_no_depth",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false));

    private static final RenderType NO_DEPTH_LINES = create("lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(3f)))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));


    private static final RenderStateShard.ShaderStateShard GUI_TEXTURE_SHADER = new RenderStateShard.ShaderStateShard(
            LDLibShaders::getGuiTexture);

    private static final Function<ResourceLocation, RenderType> GUI_TEXTURE = Util.memoize(
            texture -> create(
                    "gui_texture",
                    DefaultVertexFormat.POSITION_TEX_COLOR,
                    VertexFormat.Mode.QUADS,
                    1536,
                    CompositeState.builder()
                            .setShaderState(GUI_TEXTURE_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)
            )
    );

    private static final RenderStateShard.ShaderStateShard HSB_SHADER = new RenderStateShard.ShaderStateShard(
            LDLibShaders::getHsbShader);

    private static final RenderType HSB = create("hsb",
            LDLibShaders.HSB_VERTEX_FORMAT, VertexFormat.Mode.QUADS, 256, false, false,
            CompositeState.builder()
                    .setShaderState(HSB_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    private static final RenderType RECT = create("rect",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES,
            1536, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_GUI_OVERLAY_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    private static final RenderType STRIP_LINES = create("stripLines",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP,
            1536, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_GUI_OVERLAY_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    /**
     * Pushes the SDF tuning uniforms right before the shader is used. The supplier runs at draw time, which is
     * before {@code ShaderInstance#apply} uploads the uniform values.
     */
    private static final RenderStateShard.ShaderStateShard SDF_TEXT_SHADER = new RenderStateShard.ShaderStateShard(() -> {
        var shader = LDLibShaders.getSdfText();
        if (shader != null) {
            var sharpness = shader.getUniform("Sharpness");
            if (sharpness != null) sharpness.set(LDLibClientConfig.sharpness());
            var weight = shader.getUniform("Weight");
            if (weight != null) weight.set(LDLibClientConfig.weight());
        }
        return shader;
    });

    /**
     * These mirror vanilla's {@code RenderType.text*} states one for one, only swapping the shader and turning
     * on linear filtering, so LDLib text layers against the rest of the GUI exactly like vanilla text does.
     */
    private static final Function<ResourceLocation, RenderType> SDF_TEXT = Util.memoize(
            texture -> create("ldlib_sdf_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS, 786432, false, true,
                    sdfTextState(texture).createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> SDF_TEXT_SEE_THROUGH = Util.memoize(
            texture -> create("ldlib_sdf_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS, 1536, false, true,
                    sdfTextState(texture)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> SDF_TEXT_POLYGON_OFFSET = Util.memoize(
            texture -> create("ldlib_sdf_text_polygon_offset", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS, 1536, false, true,
                    sdfTextState(texture)
                            .setLayeringState(POLYGON_OFFSET_LAYERING)
                            .createCompositeState(false)));

    private static CompositeState.CompositeStateBuilder sdfTextState(ResourceLocation texture) {
        return CompositeState.builder()
                .setShaderState(SDF_TEXT_SHADER)
                // blur = true so the atlas is sampled with GL_LINEAR, which the distance field relies on
                .setTextureState(new TextureStateShard(texture, true, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP);
    }

    private static final RenderStateShard.ShaderStateShard GRAPH_WIRE_SHADER = new RenderStateShard.ShaderStateShard(
            LDLibShaders::getGraphWireShader);
    private static final RenderType GRAPH_WIRE = create("graphWire",
            DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLE_STRIP,
            1536, false, false,
            CompositeState.builder()
                    .setShaderState(GRAPH_WIRE_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    public LDLibRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType positionColorNoDepth() {
        return POSITION_COLOR_NO_DEPTH;
    }

    public static RenderType noDepthLines() {
        return NO_DEPTH_LINES;
    }

    public static RenderType guiTexture(ResourceLocation location) {
        return GUI_TEXTURE.apply(location);
    }

    public static RenderType hsb() {
        return HSB;
    }

    public static RenderType rect() {
        return RECT;
    }

    public static RenderType stripLines() {
        return STRIP_LINES;
    }

    public static RenderType graphWire() {
        return GRAPH_WIRE;
    }

    private static final RenderStateShard.ShaderStateShard RASTER_TEXT_SHADER =
            new RenderStateShard.ShaderStateShard(LDLibShaders::getRasterText);

    private static final Function<ResourceLocation, RenderType> RASTER_TEXT = Util.memoize(
            texture -> create("ldlib_raster_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS, 786432, false, true,
                    CompositeState.builder()
                            .setShaderState(RASTER_TEXT_SHADER)
                            // nearest: the glyph was rasterized at the size it is drawn at, so one texel is
                            // one device pixel and filtering would only give back the softness this avoids
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setLightmapState(LIGHTMAP)
                            .createCompositeState(false)));

    /**
     * Render types for glyphs rasterized at the size they are drawn at. Unlike the distance field they need no
     * see-through or polygon offset variants of their own, so one type covers all three display modes.
     */
    public static GlyphRenderTypes rasterTextGlyphs(ResourceLocation atlasPage) {
        var type = RASTER_TEXT.apply(atlasPage);
        return new GlyphRenderTypes(type, type, type);
    }

    /**
     * Render types for glyphs baked into an SDF glyph atlas page. Mirrors the three display modes vanilla
     * expects from {@link net.minecraft.client.gui.font.GlyphRenderTypes}.
     */
    public static GlyphRenderTypes sdfTextGlyphs(ResourceLocation atlasPage) {
        return new GlyphRenderTypes(SDF_TEXT.apply(atlasPage), SDF_TEXT_SEE_THROUGH.apply(atlasPage),
                SDF_TEXT_POLYGON_OFFSET.apply(atlasPage));
    }
}
