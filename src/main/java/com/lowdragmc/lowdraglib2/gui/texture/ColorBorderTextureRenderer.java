package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ColorBorderTextureRenderer {
    private ColorBorderTextureRenderer() {
    }

    public static void draw(ColorBorderTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, ColorBorderTextureRenderer::drawInternal);
    }

    private static void drawInternal(ColorBorderTexture texture, GUIContext context, float x, float y, float width, float height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (texture.border >= 0) {
            DrawerHelperClient.drawSolidRect(context, x - texture.border, y + height, width + 2f * texture.border, texture.border, texture.color);
            DrawerHelperClient.drawSolidRect(context, x - texture.border, y, texture.border, height, texture.color);
            DrawerHelperClient.drawSolidRect(context, x + width, y, texture.border, height, texture.color);
            DrawerHelperClient.drawSolidRect(context, x - texture.border, y - texture.border, width + 2f * texture.border, texture.border, texture.color);
        } else {
            float absBorder = Math.abs(texture.border);
            DrawerHelperClient.drawSolidRect(context, x, y, width - absBorder, absBorder, texture.color);
            DrawerHelperClient.drawSolidRect(context, x, y + absBorder, absBorder, height - absBorder, texture.color);
            DrawerHelperClient.drawSolidRect(context, x + absBorder, y + height - absBorder, width - absBorder, absBorder, texture.color);
            DrawerHelperClient.drawSolidRect(context, x + width - absBorder, y, absBorder, height - absBorder, texture.color);
        }
    }
}
