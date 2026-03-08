package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.SpriteTextureClientSupport;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GraphViewRenderer {
    private GraphViewRenderer() {
    }

    public static void drawBackgroundAdditional(GraphView graphView, GUIContext context) {
        var x = graphView.getContentX();
        var y = graphView.getContentY();
        var w = graphView.getContentWidth();
        var h = graphView.getContentHeight();

        var imageWidth = graphView.getGraphViewStyle().gridSize();
        var imageHeight = graphView.getGraphViewStyle().gridSize();
        var gridSize = graphView.getGraphViewStyle().gridSize();

        if (graphView.getGraphViewStyle().gridTexture() instanceof SpriteTexture spriteTexture) {
            imageWidth = SpriteTextureClientSupport.getImageSize(spriteTexture).width;
            imageHeight = SpriteTextureClientSupport.getImageSize(spriteTexture).height;
        }

        context.pose.pushPose();

        float worldLeft = graphView.getOffsetX();
        float worldTop = graphView.getOffsetY();
        float worldRight = graphView.getOffsetX() + w / graphView.getScale();
        float worldBottom = graphView.getOffsetY() + h / graphView.getScale();

        float gridStartX = (float) Math.floor(worldLeft / gridSize) * gridSize;
        float gridStartY = (float) Math.floor(worldTop / gridSize) * gridSize;

        float gridEndX = (float) Math.ceil(worldRight / gridSize) * gridSize;
        float gridEndY = (float) Math.ceil(worldBottom / gridSize) * gridSize;

        context.pose.translate(x, y);
        context.pose.scale(graphView.getScale(), graphView.getScale());
        context.pose.translate(-graphView.getOffsetX(), -graphView.getOffsetY());

        float textureScaleX = gridSize / imageWidth;
        float textureScaleY = gridSize / imageHeight;

        context.pose.scale(textureScaleX, textureScaleY);

        float drawX = gridStartX / textureScaleX;
        float drawY = gridStartY / textureScaleY;
        float drawW = (gridEndX - gridStartX) / textureScaleX;
        float drawH = (gridEndY - gridStartY) / textureScaleY;

        context.drawTexture(graphView.getGraphViewStyle().gridTexture(), drawX, drawY, drawW, drawH);

        context.pose.popPose();
    }
}
