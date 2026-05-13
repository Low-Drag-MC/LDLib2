package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

@FunctionalInterface
public interface GuiTexture extends IGuiTexture {
    void draw(GUIContext context, float x, float y, float width, float height);

    static GuiTexture of(GuiTexture texture) {
        return texture;
    }

    @Override
    default IGuiTexture copy() {
        return this;
    }
}
