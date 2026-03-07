package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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

    @Override
    public void draw(GUIContext context, float x, float y, float width, float height) {
        textureSupplier.get().draw(context, x, y, width, height);
    }
}
