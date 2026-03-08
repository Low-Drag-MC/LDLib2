package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UIResourceTextureRenderer {
    private UIResourceTextureRenderer() {
    }

    public static void draw(UIResourceTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, UIResourceTextureRenderer::drawInternal);
    }

    private static void drawInternal(UIResourceTexture texture, GUIContext context, float x, float y, float width, float height) {
        context.drawTexture(texture.getInternalTexture(), x, y, width, height);
    }
}
