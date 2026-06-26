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

import static net.minecraft.client.renderer.RenderPipelines.*;

public class LDLibRenderPipelines {
    public static final RenderPipeline GUI_TRIANGLE = RenderPipeline.builder(GUI_SNIPPET)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withLocation(LDLib2.id("pipeline/gui_triangle"))
            .build();

    public static final RenderPipeline POSITION_COLOR_NO_DEPTH = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withLocation(LDLib2.id("pipeline/position_color_no_depth"))
            .build();

    public static final RenderPipeline BLOCK_OVERLAY = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withLocation(LDLib2.id("pipeline/block_overlay"))
            .build();

    public static final RenderPipeline NO_DEPTH_LINES = RenderPipeline.builder(LINES_SNIPPET)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withLocation(LDLib2.id("pipeline/no_depth_lines"))
            .build();

    public static final RenderPipeline GRAPH_WIRE = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader(LDLib2.id("core/graph_wire"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withLocation(LDLib2.id("pipeline/graph_wire"))
            .build();

    public static final RenderPipeline ROUNDED_RECT = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader(LDLib2.id("core/rounded_rect"))
            .withFragmentShader(LDLib2.id("core/rounded_rect"))
            .withVertexBinding(0, LDLibShaders.ROUNDED_RECT_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withLocation(LDLib2.id("pipeline/rounded_rect"))
            .build();

    public static final RenderPipeline HSB = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader(LDLib2.id("core/hsb_block"))
            .withFragmentShader(LDLib2.id("core/hsb_block"))
            .withVertexBinding(0, LDLibShaders.HSB_VERTEX_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
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
    public static final RenderPipeline MASK_ALPHA_MULTIPLY = RenderPipeline.builder()
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
            .build();

    public static final RenderPipeline STRIP_LINES = RenderPipeline.builder(GUI_SNIPPET)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withLocation(LDLib2.id("pipeline/strip_lines"))
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
    }
}
