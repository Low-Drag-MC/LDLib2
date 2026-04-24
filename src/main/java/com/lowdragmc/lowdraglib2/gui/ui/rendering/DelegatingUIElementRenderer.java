package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;

public abstract class DelegatingUIElementRenderer<T extends UIElement, S extends DelegatingUIElementRenderer<T, S>> implements RegisteredUIElementRenderer<T, S> {
    @Override
    public void drawContents(T element, IGUIContext context) {
        RegisteredUIElementRenderer.super.drawContents(element, context);
    }

    @Override
    public void drawBackgroundAdditional(T element, IGUIContext context) {
        RegisteredUIElementRenderer.super.drawBackgroundAdditional(element, context);
    }

    @Override
    public void drawBackgroundOverlay(T element, IGUIContext context) {
        RegisteredUIElementRenderer.super.drawBackgroundOverlay(element, context);
    }

    protected final void drawParentContents(T element, IGUIContext context) {
        UIElementRendererRegistry.findParentRendererByType(type()).drawContents(element, context);
    }

    protected final void drawParentBackgroundAdditional(T element, IGUIContext context) {
        UIElementRendererRegistry.findParentRendererByType(type()).drawBackgroundAdditional(element, context);
    }

    protected final void drawParentBackgroundOverlay(T element, IGUIContext context) {
        UIElementRendererRegistry.findParentRendererByType(type()).drawBackgroundOverlay(element, context);
    }
}
