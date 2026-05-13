package com.lowdragmc.lowdraglib2.client.shader;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
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

    public static final RenderPipeline STRIP_LINES = RenderPipeline.builder(GUI_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
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
        event.registerPipeline(STRIP_LINES);
    }
}
