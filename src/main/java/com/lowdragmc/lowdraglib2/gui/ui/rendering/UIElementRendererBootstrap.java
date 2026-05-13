package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;

public final class UIElementRendererBootstrap {
    private UIElementRendererBootstrap() {
    }

    public static void applyEntries(Iterable<? extends RegisteredUIElementRenderer<?, ?>> renderers) {
        for (var renderer : renderers) {
            applyRenderer(renderer);
        }
    }

    public static void applyRegistry(AutoRegistry.LDLibRegisterClient<RegisteredUIElementRenderer, RegisteredUIElementRenderer> registry) {
        for (var holder : registry) {
            applyRenderer(holder.value());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends UIElement> void applyRenderer(RegisteredUIElementRenderer<?, ?> renderer) {
        UIElementRendererRegistry.register((Class<T>) renderer.type(), (UIElementRenderer<? super T>) renderer);
    }
}
