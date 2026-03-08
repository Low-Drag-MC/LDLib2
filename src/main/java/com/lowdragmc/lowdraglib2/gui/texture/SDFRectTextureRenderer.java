package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SDFRectTextureRenderer {
    private SDFRectTextureRenderer() {
    }

    public static void draw(SDFRectTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, SDFRectTextureRenderer::drawInternal);
    }

    private static void drawInternal(SDFRectTexture texture, GUIContext context, float x, float y, float width, float height) {
        if (ColorUtils.alpha(texture.getColor()) > 0) {
            context.fillRoundedRect(x, y, width, height, texture.getRadius(), texture.getColor());
        }
        if (texture.getStroke() > 0 && ColorUtils.alpha(texture.getBorderColor()) > 0) {
            context.borderRoundedRect(x, y, width, height, texture.getRadius(), texture.getStroke(), texture.getBorderColor());
        }
    }
}
