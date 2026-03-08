package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TextTextureRenderer {
    private TextTextureRenderer() {
    }

    public static void draw(TextTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, TextTextureRenderer::drawInternal);
    }

    private static void drawInternal(TextTexture texture, GUIContext context, float x, float y, float width, float height) {
        TextTextureClientSupport.draw(texture, context, x, y, width, height);
    }
}
