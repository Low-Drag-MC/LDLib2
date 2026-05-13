package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.GuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

public final class DefaultGuiTextureRenderer implements GuiTextureRenderer<IGuiTexture> {
    public static final DefaultGuiTextureRenderer INSTANCE = new DefaultGuiTextureRenderer();

    private DefaultGuiTextureRenderer() {
    }

    @Override
    public void draw(IGuiTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
        if (texture instanceof GuiTexture guiTexture) {
            guiTexture.draw(context, x, y, width, height);
        }
    }
}
