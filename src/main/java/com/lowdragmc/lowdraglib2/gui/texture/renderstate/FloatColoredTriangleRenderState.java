package com.lowdragmc.lowdraglib2.gui.texture.renderstate;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record FloatColoredTriangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    Vector2f position0,
    Vector2f position1,
    Vector2f position2,
    int color0,
    int color1,
    int color2,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public FloatColoredTriangleRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        Vector2f position0,
        Vector2f position1,
        Vector2f position2,
        int color0,
        int color1,
        int color2,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, position0, position1, position2, color0, color1, color2, scissorArea,
                getBounds(position0, position1, position2, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(this.pose(), this.position0().x, this.position0().y).setColor(this.color0());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.position1().x, this.position1().y).setColor(this.color1());
        vertexConsumer.addVertexWith2DPose(this.pose(), this.position2().x, this.position2().y).setColor(this.color2());
    }

    public static @Nullable ScreenRectangle getBounds(Vector2f position0,
                                                      Vector2f position1,
                                                      Vector2f position2,
                                                      Matrix3x2fc pose,
                                                      @Nullable ScreenRectangle scissorArea) {
        float minX = Math.min(position0.x, Math.min(position1.x, position2.x));
        float minY = Math.min(position0.y, Math.min(position1.y, position2.y));
        float maxX = Math.max(position0.x, Math.max(position1.x, position2.x));
        float maxY = Math.max(position0.y, Math.max(position1.y, position2.y));
        return FloatBlitRenderState.getBounds(minX, minY, maxX, maxY, pose, scissorArea);
    }
}
