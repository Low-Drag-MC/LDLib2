package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class VanillaSpriteTextureRenderer {
    private VanillaSpriteTextureRenderer() {
    }

    public static void draw(VanillaSpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, VanillaSpriteTextureRenderer::drawInternal);
    }

    private static void drawInternal(VanillaSpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
        var sprite = texture.getSprite();
        if (sprite == null || width <= 0 || height <= 0) {
            return;
        }
        context.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, texture.getColor());
    }
}
