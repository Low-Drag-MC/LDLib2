package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;

import java.util.function.Supplier;

@KJSBindings
public class DynamicTexture implements IGuiTexture {
    public Supplier<IGuiTexture> textureSupplier;

    public DynamicTexture(Supplier<IGuiTexture> rendererSupplier) {
        this.textureSupplier = rendererSupplier;
    }

    public static DynamicTexture of(Supplier<IGuiTexture> rendererSupplier) {
        return new DynamicTexture(rendererSupplier);
    }
}
