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
    /**
     * How far the quad reaches past the shape, in the same units as the rect: the anti-aliased edge fades out across
     * the outline, so without it the outer half of that fade has no pixels to land on and the edge comes out hard.
     */
    private static final float AA_MARGIN = 1f;

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
                // the shape's own bounds, not the quad's: bounds only pick the layer, and with the margin in them rects
                // that merely touch (list rows, tab strips) would overlap and stack a layer each, breaking batching
                scissorArea, FloatBlitRenderState.getBounds(x, y, x + width, y + height, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        float halfW = width / 2f;
        float halfH = height / 2f;
        // the quad, which the shape sits inside of with a margin all round (see AA_MARGIN), in shape-local space
        float qx = halfW + AA_MARGIN, qy = halfH + AA_MARGIN;
        float cx = x + halfW, cy = y + halfH;

        // Encode as fixed-point × 8
        short hws = (short) Math.round(halfW * 8.0);
        short hhs = (short) Math.round(halfH * 8.0);
        short bs  = (short) Math.round(border * 8.0);
        short rTL = (short) Math.round(radiusTL * 8.0);
        short rTR = (short) Math.round(radiusTR * 8.0);
        short rBR = (short) Math.round(radiusBR * 8.0);
        short rBL = (short) Math.round(radiusBL * 8.0);

        // Emit 4 vertices: TL, BL, BR, TR (standard quad winding)
        emitVertex(vertexConsumer, cx, cy, -qx, -qy, hws, hhs, bs, rTL, rTR, rBR, rBL);
        emitVertex(vertexConsumer, cx, cy, -qx,  qy, hws, hhs, bs, rTL, rTR, rBR, rBL);
        emitVertex(vertexConsumer, cx, cy,  qx,  qy, hws, hhs, bs, rTL, rTR, rBR, rBL);
        emitVertex(vertexConsumer, cx, cy,  qx, -qy, hws, hhs, bs, rTL, rTR, rBR, rBL);
    }

    /**
     * @param lx the vertex's position relative to the shape's centre, carried to the shader in UV0. The shader can't
     *           work it out from {@code gl_VertexID}: the 26.2 GUI packs every draw into one shared buffer and draws
     *           each with a per-frame baseVertex, so the vertex index isn't the corner index.
     */
    private void emitVertex(VertexConsumer vc, float cx, float cy, float lx, float ly,
                            short hws, short hhs, short bs,
                            short rTL, short rTR, short rBR, short rBL) {
        vc.addVertexWith2DPose(this.pose, cx + lx, cy + ly).setColor(this.color).setUv(lx, ly);
        if (vc instanceof BufferBuilderAccessor accessor) {
            // 26.2: BufferBuilder only knows the 7 standard element names, so custom attributes are
            // written directly at vertexPointer + element.offset() (offsets resolved by name).
            long base = accessor.getVertexPointer();
            if (base != -1L) {
                long i = base + LDLibShaders.RECT_PARAMS_OFFSET;
                MemoryUtil.memPutShort(i, hws);
                MemoryUtil.memPutShort(i + 2L, hhs);
                MemoryUtil.memPutShort(i + 4L, bs);
                MemoryUtil.memPutShort(i + 6L, (short) 0);
                long j = base + LDLibShaders.RECT_RADIUS_OFFSET;
                MemoryUtil.memPutShort(j, rTL);
                MemoryUtil.memPutShort(j + 2L, rTR);
                MemoryUtil.memPutShort(j + 4L, rBR);
                MemoryUtil.memPutShort(j + 6L, rBL);
            }
        }
    }
}
