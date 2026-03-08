package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GuiTextureRendererBootstrap {
    private GuiTextureRendererBootstrap() {
    }

    public static void applyEntries(Iterable<? extends RegisteredGuiTextureRenderer<?, ?>> renderers) {
        for (var renderer : renderers) {
            applyRenderer(renderer);
        }
    }

    public static void applyRegistry(AutoRegistry.LDLibRegisterClient<RegisteredGuiTextureRenderer, RegisteredGuiTextureRenderer> registry) {
        for (var holder : registry) {
            applyRenderer(holder.value());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends IGuiTexture> void applyRenderer(RegisteredGuiTextureRenderer<?, ?> renderer) {
        GuiTextureRendererRegistry.register((Class<T>) renderer.type(), (GuiTextureRenderer<? super T>) renderer);
    }
}
