package com.lowdragmc.lowdraglib2.gui.texture.renderstate;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record FloatTiledBlitRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    float  tileWidth,
    float  tileHeight,
    float x0,
    float y0,
    float x1,
    float y1,
    float u0,
    float u1,
    float v0,
    float v1,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public FloatTiledBlitRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float  tileWidth,
        float  tileHeight,
        float x0,
        float y0,
        float x1,
        float y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
                pipeline,
                textureSetup,
                pose,
                tileWidth,
                tileHeight,
                x0,
                y0,
                x1,
                y1,
                u0,
                u1,
                v0,
                v1,
                color,
                scissorArea,
                FloatBlitRenderState.getBounds(x0, y0, x1, y1, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(@NotNull VertexConsumer vertexConsumer) {
        float width  = x1 - x0;
        float height = y1 - y0;

        for (float tx = 0; tx < width; tx += tileWidth) {
            float w = Math.min(tileWidth, width - tx);
            float u1 = (w == tileWidth)
                    ? this.u1
                    : Mth.lerp(w / tileWidth, this.u0, this.u1);

            for (float ty = 0; ty < height; ty += tileHeight) {
                float h = Math.min(tileHeight, height - ty);
                float v1 = (h == tileHeight)
                        ? this.v1
                        : Mth.lerp(h / tileHeight, this.v0, this.v1);

                float ax0 = x0 + tx;
                float ax1 = ax0 + w;
                float ay0 = y0 + ty;
                float ay1 = ay0 + h;

                vertexConsumer.addVertexWith2DPose(pose, ax0, ay0).setUv(u0, v0).setColor(color);
                vertexConsumer.addVertexWith2DPose(pose, ax0, ay1).setUv(u0, v1).setColor(color);
                vertexConsumer.addVertexWith2DPose(pose, ax1, ay1).setUv(u1, v1).setColor(color);
                vertexConsumer.addVertexWith2DPose(pose, ax1, ay0).setUv(u1, v0).setColor(color);
            }
        }
    }

}
