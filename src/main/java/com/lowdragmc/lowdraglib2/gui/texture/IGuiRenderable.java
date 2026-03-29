package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
public interface IGuiRenderable extends IGuiTexture {
    @Override
    @Environment(EnvType.CLIENT)
    default void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {

    }

    void draw(GUIContext context, float x, float y, float width, float height);
}
