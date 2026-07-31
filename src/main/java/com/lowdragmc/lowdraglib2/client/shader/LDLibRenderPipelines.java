package com.lowdragmc.lowdraglib2.client.shader;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.minecraft.client.renderer.RenderPipelines.*;

public class LDLibRenderPipelines {
    /**
     * Every pipeline declared below, in declaration order, so {@link #register(RegisterRenderPipelinesEvent)}
     * cannot fall out of sync with the fields. Must stay above them: they populate it during class init.
     */
    private static final List<RenderPipeline> PIPELINES = new ArrayList<>();

    private static RenderPipeline register(RenderPipeline pipeline) {
        PIPELINES.add(pipeline);
        return pipeline;
    }

    public static final RenderPipeline GUI_TRIANGLE = register(RenderPipeline.builder(GUI_SNIPPET)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withLocation(LDLib2.id("pipeline/gui_triangle"))
            .build());

    public static final RenderPipeline POSITION_COLOR_NO_DEPTH = register(RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withLocation(LDLib2.id("pipeline/position_color_no_depth"))
            .build());

    public static final RenderPipeline BLOCK_OVERLAY = register(RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withLocation(LDLib2.id("pipeline/block_overlay"))
            .build());

    public static final RenderPipeline NO_DEPTH_LINES = register(RenderPipeline.builder(LINES_SNIPPET)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withLocation(LDLib2.id("pipeline/no_depth_lines"))
            .build());

    public static final RenderPipeline GRAPH_WIRE = register(RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader(LDLib2.id("core/graph_wire"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withLocation(LDLib2.id("pipeline/graph_wire"))
            .build());

    public static final RenderPipeline ROUNDED_RECT = register(RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader(LDLib2.id("core/rounded_rect"))
            .withFragmentShader(LDLib2.id("core/rounded_rect"))
            .withVertexBinding(0, LDLibShaders.ROUNDED_RECT_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withLocation(LDLib2.id("pipeline/rounded_rect"))
            .build());

    public static final RenderPipeline HSB = register(RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader(LDLib2.id("core/hsb_block"))
            .withFragmentShader(LDLib2.id("core/hsb_block"))
            .withVertexBinding(0, LDLibShaders.HSB_VERTEX_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withLocation(LDLib2.id("pipeline/hsb"))
            .build());

    /**
     * Multiplies destination color and alpha by mask sample's factor. Used by visual-layer
     * mask baking: after a UI subtree is rendered into an off-target, draw the mask
     * with this pipeline to punch the mask shape into the off-target while preserving
     * premultiplied-alpha semantics for the final GUI_TEXTURED_PREMULTIPLIED_ALPHA blit.
     * Blend: (ZERO, SRC_ALPHA, ZERO, SRC_ALPHA) -> dst.rgba *= src.alpha.
     */
    public static final RenderPipeline MASK_ALPHA_MULTIPLY = register(RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader(LDLib2.id("core/mask_alpha_multiply"))
            .withFragmentShader(LDLib2.id("core/mask_alpha_multiply"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(new BlendFunction(
                    BlendFactor.ZERO, BlendFactor.SRC_ALPHA,
                    BlendFactor.ZERO, BlendFactor.SRC_ALPHA)))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withLocation(LDLib2.id("pipeline/mask_alpha_multiply"))
            .build());

    public static final RenderPipeline STRIP_LINES = register(RenderPipeline.builder(GUI_SNIPPET)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withLocation(LDLib2.id("pipeline/strip_lines"))
            .build());

    /**
     * The parts every LDLib text pipeline shares. Deliberately not built on vanilla's {@code TEXT_SNIPPET}: that
     * one pulls in {@link BindGroupLayouts#GLOBALS}, which neither {@code sdf_text} nor {@code raster_text}
     * declares. No depth state here, so each pipeline below states its own (and the GUI ones state none, which
     * is what turns depth testing off).
     */
    private static final RenderPipeline.Snippet LD_TEXT_SNIPPET = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, LDLibShaders.SDF_TEXT_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .buildSnippet();

    /**
     * Pushes text towards the viewer so it does not z-fight the surface it is drawn on.
     * <p>
     * 26.2 renders the world with a reversed depth range, so "towards the viewer" is a positive bias tested with
     * {@code GREATER_THAN_OR_EQUAL} - the mirror of the negative bias earlier versions used. Same values as
     * vanilla's own {@code TEXT_POLYGON_OFFSET}.
     */
    private static final DepthStencilState TEXT_POLYGON_OFFSET_DEPTH =
            new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F);

    /** Draws over whatever is already there without disturbing the depth buffer. */
    private static final DepthStencilState TEXT_SEE_THROUGH_DEPTH =
            new DepthStencilState(CompareOp.ALWAYS_PASS, false);

    private static RenderPipeline textPipeline(String shader, String variant, @Nullable DepthStencilState depth) {
        return register(RenderPipeline.builder(LD_TEXT_SNIPPET)
                .withVertexShader(LDLib2.id("core/" + shader))
                .withFragmentShader(LDLib2.id("core/" + shader))
                .withDepthStencilState(Optional.ofNullable(depth))
                .withLocation(LDLib2.id("pipeline/" + shader + variant))
                .build());
    }

    /**
     * Glyphs sampled from a signed distance field atlas.
     * <p>
     * The GUI variant drops depth testing the way vanilla's {@code GUI_TEXT} does, the world variant keeps it so
     * text drawn into a scene still sorts against geometry. Both need the atlas sampled with {@code LINEAR},
     * which is what reconstructs the field between texels; that is a property of the sampler rather than the
     * pipeline, so it is set where the texture is bound.
     */
    public static final RenderPipeline SDF_TEXT_GUI = textPipeline("sdf_text", "_gui", null);
    public static final RenderPipeline SDF_TEXT = textPipeline("sdf_text", "", DepthStencilState.DEFAULT);
    public static final RenderPipeline SDF_TEXT_POLYGON_OFFSET =
            textPipeline("sdf_text", "_polygon_offset", TEXT_POLYGON_OFFSET_DEPTH);
    public static final RenderPipeline SDF_TEXT_SEE_THROUGH =
            textPipeline("sdf_text", "_see_through", TEXT_SEE_THROUGH_DEPTH);

    /**
     * Glyphs rasterized at the size they are drawn at. One texel is one device pixel, so the atlas is sampled
     * with {@code NEAREST} and the shader only has to unpack the coverage value.
     */
    public static final RenderPipeline RASTER_TEXT_GUI = textPipeline("raster_text", "_gui", null);
    public static final RenderPipeline RASTER_TEXT = textPipeline("raster_text", "", DepthStencilState.DEFAULT);
    public static final RenderPipeline RASTER_TEXT_POLYGON_OFFSET =
            textPipeline("raster_text", "_polygon_offset", TEXT_POLYGON_OFFSET_DEPTH);
    public static final RenderPipeline RASTER_TEXT_SEE_THROUGH =
            textPipeline("raster_text", "_see_through", TEXT_SEE_THROUGH_DEPTH);

    public static void register(RegisterRenderPipelinesEvent event) {
        PIPELINES.forEach(event::registerPipeline);
    }
}
