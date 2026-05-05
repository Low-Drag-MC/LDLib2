package com.lowdragmc.lowdraglib2.gui.texture.renderstate;

import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public record FloatLineStripRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        List<Vector2f> points,
        int colorStart,
        int colorEnd,
        float halfWidth,
        boolean textured,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public FloatLineStripRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            List<Vector2f> points,
            int colorStart,
            int colorEnd,
            float halfWidth,
            boolean textured,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                pipeline,
                textureSetup,
                pose,
                copyPoints(points),
                colorStart,
                colorEnd,
                halfWidth,
                textured,
                scissorArea,
                getBounds(points, halfWidth, pose, scissorArea)
        );
    }

    public FloatLineStripRenderState {
        points = copyPoints(points);
    }

    @Override
    public void buildVertices(@NotNull VertexConsumer vertexConsumer) {
        if (textured) {
            RenderBufferUtils.drawColorTexLines(pose, vertexConsumer, points, colorStart, colorEnd, halfWidth, true);
        } else {
            RenderBufferUtils.drawColorLines(pose, vertexConsumer, points, colorStart, colorEnd, halfWidth, true);
        }
    }

    private static List<Vector2f> copyPoints(List<Vector2f> points) {
        var copied = new ArrayList<Vector2f>(points.size());
        for (var point : points) {
            copied.add(new Vector2f(point));
        }
        return List.copyOf(copied);
    }

    private static @Nullable ScreenRectangle getBounds(
            List<Vector2f> points,
            float halfWidth,
            Matrix3x2fc pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        if (points.size() < 2) {
            return null;
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (var point : points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        float padding = Math.max(halfWidth, 1.0f);
        return FloatBlitRenderState.getBounds(
                minX - padding,
                minY - padding,
                maxX + padding,
                maxY + padding,
                pose,
                scissorArea
        );
    }
}
