package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

public final class ColorSelectorTextures {
    private static Provider provider = Provider.EMPTY;

    private ColorSelectorTextures() {}

    public static void register(Provider provider) {
        ColorSelectorTextures.provider = provider;
    }

    public static IGuiTexture colorPreview(ColorSelector selector) {
        return DynamicTexture.of(() -> provider.colorPreview(selector));
    }

    public static IGuiTexture hsbContext(ColorSelector selector) {
        return DynamicTexture.of(() -> provider.hsbContext(selector));
    }

    public static IGuiTexture colorSlider(ColorSelector selector) {
        return DynamicTexture.of(() -> provider.colorSlider(selector));
    }

    public static IGuiTexture alphaSlider(ColorSelector selector) {
        return DynamicTexture.of(() -> provider.alphaSlider(selector));
    }

    public interface Provider {
        Provider EMPTY = new Provider() {
            @Override
            public IGuiTexture colorPreview(ColorSelector selector) {
                return IGuiTexture.EMPTY;
            }

            @Override
            public IGuiTexture hsbContext(ColorSelector selector) {
                return IGuiTexture.EMPTY;
            }

            @Override
            public IGuiTexture colorSlider(ColorSelector selector) {
                return IGuiTexture.EMPTY;
            }

            @Override
            public IGuiTexture alphaSlider(ColorSelector selector) {
                return IGuiTexture.EMPTY;
            }
        };

        IGuiTexture colorPreview(ColorSelector selector);

        IGuiTexture hsbContext(ColorSelector selector);

        IGuiTexture colorSlider(ColorSelector selector);

        IGuiTexture alphaSlider(ColorSelector selector);
    }
}
