package com.lowdragmc.lowdraglib2.client.shader;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import static net.minecraft.client.renderer.RenderPipelines.*;

public class LDLibRenderPipelines {
    public static final RenderPipeline GUI_TRIANGLE = RenderPipeline.builder(GUI_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withLocation(LDLib2.id("pipeline/gui_triangle")).build();

    public static final RenderPipeline POSITION_COLOR_NO_DEPTH = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withLocation(LDLib2.id("pipeline/position_color_no_depth"))
            .build();

    public static final RenderPipeline BLOCK_OVERLAY = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/block_overlay"))
            .build();

    public static final RenderPipeline NO_DEPTH_LINES = RenderPipeline.builder(LINES_SNIPPET)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withLocation(LDLib2.id("pipeline/no_depth_lines"))
            .build();

    public static final RenderPipeline GRAPH_WIRE = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader(LDLib2.id("core/graph_wire"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            .withLocation(LDLib2.id("pipeline/graph_wire"))
            .build();

    public static final RenderPipeline ROUNDED_RECT = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/rounded_rect"))
            .withFragmentShader(LDLib2.id("core/rounded_rect"))
            .withVertexFormat(LDLibShaders.ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withLocation(LDLib2.id("pipeline/rounded_rect"))
            .build();

    public static final RenderPipeline HSB = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/hsb_block"))
            .withFragmentShader(LDLib2.id("core/hsb_block"))
            .withVertexFormat(LDLibShaders.HSB_VERTEX_FORMAT, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withLocation(LDLib2.id("pipeline/hsb"))
            .build();

    /**
     * Multiplies destination color and alpha by mask sample's factor. Used by visual-layer
     * mask baking: after a UI subtree is rendered into an off-target, draw the mask
     * with this pipeline to punch the mask shape into the off-target while preserving
     * premultiplied-alpha semantics for the final GUI_TEXTURED_PREMULTIPLIED_ALPHA blit.
     * Blend: (ZERO, SRC_ALPHA, ZERO, SRC_ALPHA) -> dst.rgba *= src.alpha.
     */
    public static final RenderPipeline MASK_ALPHA_MULTIPLY = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/mask_alpha_multiply"))
            .withFragmentShader(LDLib2.id("core/mask_alpha_multiply"))
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(new BlendFunction(
                    SourceFactor.ZERO, DestFactor.SRC_ALPHA,
                    SourceFactor.ZERO, DestFactor.SRC_ALPHA)))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/mask_alpha_multiply"))
            .build();

    public static final RenderPipeline STRIP_LINES = RenderPipeline.builder(GUI_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withLocation(LDLib2.id("pipeline/strip_lines"))
            .build();

    /**
     * Glyphs sampled from a signed distance field atlas.
     * <p>
     * The GUI variant drops depth testing the way vanilla's {@code GUI_TEXT} does, the world variant keeps it so
     * text drawn into a scene still sorts against geometry. Both need the atlas sampled with {@code LINEAR},
     * which is what reconstructs the field between texels; that is a property of the sampler rather than the
     * pipeline, so it is set where the texture is bound.
     */
    public static final RenderPipeline SDF_TEXT_GUI = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/sdf_text"))
            .withFragmentShader(LDLib2.id("core/sdf_text"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(LDLibShaders.SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/sdf_text_gui"))
            .build();

    public static final RenderPipeline SDF_TEXT = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/sdf_text"))
            .withFragmentShader(LDLib2.id("core/sdf_text"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexFormat(LDLibShaders.SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/sdf_text"))
            .build();

    /**
     * Same as {@link #SDF_TEXT} but offset towards the viewer, so text drawn on a surface does not z-fight it.
     */
    public static final RenderPipeline SDF_TEXT_POLYGON_OFFSET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/sdf_text"))
            .withFragmentShader(LDLib2.id("core/sdf_text"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -1.0F, -10.0F))
            .withVertexFormat(LDLibShaders.SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/sdf_text_polygon_offset"))
            .build();

    public static final RenderPipeline SDF_TEXT_SEE_THROUGH = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/sdf_text"))
            .withFragmentShader(LDLib2.id("core/sdf_text"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexFormat(LDLibShaders.SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/sdf_text_see_through"))
            .build();

    /**
     * Glyphs rasterized at the size they are drawn at. One texel is one device pixel, so the atlas is sampled
     * with {@code NEAREST} and the shader only has to unpack the coverage value.
     */
    public static final RenderPipeline RASTER_TEXT_GUI = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/raster_text"))
            .withFragmentShader(LDLib2.id("core/raster_text"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(LDLibShaders.SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/raster_text_gui"))
            .build();

    public static final RenderPipeline RASTER_TEXT = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/raster_text"))
            .withFragmentShader(LDLib2.id("core/raster_text"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexFormat(LDLibShaders.SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/raster_text"))
            .build();

    public static final RenderPipeline RASTER_TEXT_POLYGON_OFFSET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/raster_text"))
            .withFragmentShader(LDLib2.id("core/raster_text"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -1.0F, -10.0F))
            .withVertexFormat(LDLibShaders.SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/raster_text_polygon_offset"))
            .build();

    public static final RenderPipeline RASTER_TEXT_SEE_THROUGH = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader(LDLib2.id("core/raster_text"))
            .withFragmentShader(LDLib2.id("core/raster_text"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexFormat(LDLibShaders.SDF_TEXT_FORMAT, VertexFormat.Mode.QUADS)
            .withLocation(LDLib2.id("pipeline/raster_text_see_through"))
            .build();

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GUI_TRIANGLE);
        event.registerPipeline(POSITION_COLOR_NO_DEPTH);
        event.registerPipeline(BLOCK_OVERLAY);
        event.registerPipeline(NO_DEPTH_LINES);
        event.registerPipeline(GRAPH_WIRE);
        event.registerPipeline(ROUNDED_RECT);
        event.registerPipeline(HSB);
        event.registerPipeline(MASK_ALPHA_MULTIPLY);
        event.registerPipeline(STRIP_LINES);
        event.registerPipeline(SDF_TEXT_GUI);
        event.registerPipeline(SDF_TEXT);
        event.registerPipeline(SDF_TEXT_POLYGON_OFFSET);
        event.registerPipeline(SDF_TEXT_SEE_THROUGH);
        event.registerPipeline(RASTER_TEXT_GUI);
        event.registerPipeline(RASTER_TEXT);
        event.registerPipeline(RASTER_TEXT_POLYGON_OFFSET);
        event.registerPipeline(RASTER_TEXT_SEE_THROUGH);
    }
}
