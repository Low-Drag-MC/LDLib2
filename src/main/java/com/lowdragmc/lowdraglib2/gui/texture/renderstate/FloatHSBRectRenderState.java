package com.lowdragmc.lowdraglib2.gui.texture.renderstate;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.BufferBuilderAccessor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/**
 * Render state for a quad with per-corner HSB+Alpha values.
 * hsb00 = top-left, hsb01 = bottom-left, hsb11 = bottom-right, hsb10 = top-right.
 * Each float[4] is {h, s, b, a}.
 */
@OnlyIn(Dist.CLIENT)
public record FloatHSBRectRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    float x0, float y0, float x1, float y1,
    float[] hsb00, float[] hsb01, float[] hsb11, float[] hsb10,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public FloatHSBRectRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0, float y0, float x1, float y1,
        float[] hsb00, float[] hsb01, float[] hsb11, float[] hsb10,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x0, y0, x1, y1,
                hsb00, hsb01, hsb11, hsb10,
                scissorArea, FloatBlitRenderState.getBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        emitVertex(vertexConsumer, x0, y0, hsb00);
        emitVertex(vertexConsumer, x0, y1, hsb01);
        emitVertex(vertexConsumer, x1, y1, hsb11);
        emitVertex(vertexConsumer, x1, y0, hsb10);
    }

    private void emitVertex(VertexConsumer vc, float px, float py, float[] hsba) {
        vc.addVertexWith2DPose(this.pose, px, py);
        if (vc instanceof BufferBuilderAccessor accessor) {
            long i = accessor.invokeBeginElement(LDLibShaders.HSB_Alpha);
            if (i != -1L) {
                MemoryUtil.memPutFloat(i, hsba[0]);
                MemoryUtil.memPutFloat(i + 4L, hsba[1]);
                MemoryUtil.memPutFloat(i + 8L, hsba[2]);
                MemoryUtil.memPutFloat(i + 12L, hsba[3]);
            }
        }
    }
}