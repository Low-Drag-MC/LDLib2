package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class AnimationTextureRenderer {
    private AnimationTextureRenderer() {
    }

    public static void draw(AnimationTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, AnimationTextureRenderer::drawInternal);
    }

    private static void drawInternal(AnimationTexture texture, GUIContext context, float x, float y, float width, float height) {
        AnimationTextureClientSupport.updateTick(texture);
        float cell = 1f / texture.getCellSize();
        int frameX = texture.currentFrame % texture.getCellSize();
        int frameY = texture.currentFrame / texture.getCellSize();
        float imageU = frameX * cell;
        float imageV = frameY * cell;

        context.blit(RenderPipelines.GUI_TEXTURED, texture.imageLocation, x, y,
                width, height, imageU, imageV, imageU + cell, imageV + cell,
                texture.getColor());
    }
}
