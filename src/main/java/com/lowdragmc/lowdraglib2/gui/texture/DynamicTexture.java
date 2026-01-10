package com.lowdragmc.lowdraglib2.gui.texture;

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
    @OnlyIn(Dist.CLIENT)
    public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        textureSupplier.get().draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
    }
}
