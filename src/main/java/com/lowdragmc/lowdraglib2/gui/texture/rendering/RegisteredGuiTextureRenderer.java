package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;

public interface RegisteredGuiTextureRenderer<T extends IGuiTexture, S extends RegisteredGuiTextureRenderer<T, S>> extends GuiTextureRenderer<T>, ILDLRegisterClient<S, S> {
    Class<T> type();
}
