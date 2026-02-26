package com.lowdragmc.lowdraglib2.client.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
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
}
