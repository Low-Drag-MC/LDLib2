package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

@FunctionalInterface
public interface GuiTextureRenderer<T extends IGuiTexture> {
    void draw(T texture, GUIContext context, float x, float y, float width, float height);
}
