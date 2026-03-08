package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GuiTextureGroupRenderer {
    private GuiTextureGroupRenderer() {
    }

    public static void draw(GuiTextureGroup texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, GuiTextureGroupRenderer::drawInternal);
    }

    private static void drawInternal(GuiTextureGroup texture, GUIContext context, float x, float y, float width, float height) {
        for (IGuiTexture child : texture.getTextures()) {
            context.drawTexture(child, x, y, width, height);
        }
    }
}
