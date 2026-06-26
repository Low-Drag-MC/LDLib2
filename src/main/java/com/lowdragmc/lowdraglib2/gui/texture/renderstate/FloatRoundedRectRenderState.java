package com.lowdragmc.lowdraglib2.gui.texture.renderstate;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.BufferBuilderAccessor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public record FloatRoundedRectRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    float x, float y, float width, float height,
    float radiusTL, float radiusTR, float radiusBR, float radiusBL,
    int color,
    float border,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public FloatRoundedRectRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x, float y, float width, float height,
        float radiusTL, float radiusTR, float radiusBR, float radiusBL,
        int color,
        float border,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x, y, width, height,
                radiusTL, radiusTR, radiusBR, radiusBL, color, border,
                scissorArea, FloatBlitRenderState.getBounds(x, y, x + width, y + height, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        float x0 = x, y0 = y, x1 = x + width, y1 = y + height;
        float halfW = width / 2f;
        float halfH = height / 2f;

        // Encode as fixed-point × 8
        short hws = (short) Math.round(halfW * 8.0);
        short hhs = (short) Math.round(halfH * 8.0);
        short bs  = (short) Math.round(border * 8.0);
        short rTL = (short) Math.round(radiusTL * 8.0);
        short rTR = (short) Math.round(radiusTR * 8.0);
        short rBR = (short) Math.round(radiusBR * 8.0);
        short rBL = (short) Math.round(radiusBL * 8.0);

        // Emit 4 vertices: TL, BL, BR, TR (standard quad winding). The corner id (0..3) is the last
        // arg: the shader reads it from RectParams.w to derive the local position (it cannot rely on
        // gl_VertexID because the 26.2 GUI draws from a shared buffer with a per-frame baseVertex).
        emitVertex(vertexConsumer, x0, y0, hws, hhs, bs, rTL, rTR, rBR, rBL, (short) 0);
        emitVertex(vertexConsumer, x0, y1, hws, hhs, bs, rTL, rTR, rBR, rBL, (short) 1);
        emitVertex(vertexConsumer, x1, y1, hws, hhs, bs, rTL, rTR, rBR, rBL, (short) 2);
        emitVertex(vertexConsumer, x1, y0, hws, hhs, bs, rTL, rTR, rBR, rBL, (short) 3);
    }

    private void emitVertex(VertexConsumer vc, float px, float py,
                            short hws, short hhs, short bs,
                            short rTL, short rTR, short rBR, short rBL, short cornerId) {
        vc.addVertexWith2DPose(this.pose, px, py).setColor(this.color);
        if (vc instanceof BufferBuilderAccessor accessor) {
            // 26.2: BufferBuilder only knows the 7 standard element names, so custom attributes are
            // written directly at vertexPointer + element.offset() (offsets resolved by name).
            long base = accessor.getVertexPointer();
            if (base != -1L) {
                long i = base + LDLibShaders.RECT_PARAMS_OFFSET;
                MemoryUtil.memPutShort(i, hws);
                MemoryUtil.memPutShort(i + 2L, hhs);
                MemoryUtil.memPutShort(i + 4L, bs);
                MemoryUtil.memPutShort(i + 6L, cornerId);
                long j = base + LDLibShaders.RECT_RADIUS_OFFSET;
                MemoryUtil.memPutShort(j, rTL);
                MemoryUtil.memPutShort(j + 2L, rTR);
                MemoryUtil.memPutShort(j + 4L, rBR);
                MemoryUtil.memPutShort(j + 6L, rBL);
            }
        }
    }
}
