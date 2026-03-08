package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

public final class TreeListDraggingOverlays {
    private static Provider provider = mode -> IGuiTexture.EMPTY;

    private TreeListDraggingOverlays() {}

    public static void register(Provider provider) {
        TreeListDraggingOverlays.provider = provider;
    }

    public static IGuiTexture of(int mode) {
        return DynamicTexture.of(() -> provider.overlay(mode));
    }

    public interface Provider {
        IGuiTexture overlay(int mode);
    }
}
