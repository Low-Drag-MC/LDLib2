package com.lowdragmc.lowdraglib2.gui.texture.renderstate;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record FloatColoredRectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    float x0,
    float y0,
    float x1,
    float y1,
    int col00,
    int col01,
    int co11,
    int col10,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public FloatColoredRectangleRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0,
        float y0,
        float x1,
        float y1,
        int col00,
        int col01,
        int co11,
        int col10,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x0, y0, x1, y1, col00, col01, co11, col10, scissorArea,
                FloatBlitRenderState.getBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setColor(this.col00());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setColor(this.col01());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setColor(this.co11());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setColor(this.col10());
    }
}
