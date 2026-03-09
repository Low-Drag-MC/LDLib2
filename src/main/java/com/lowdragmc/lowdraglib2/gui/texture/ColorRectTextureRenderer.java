package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ColorRectTextureRenderer {
    private ColorRectTextureRenderer() {
    }

    public static void draw(ColorRectTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, ColorRectTextureRenderer::drawInternal);
    }

    private static void drawInternal(ColorRectTexture texture, GUIContext context, float x, float y, float width, float height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        DrawerHelperClient.drawSolidRect(context, x, y, width, height, texture.color);
    }
}
