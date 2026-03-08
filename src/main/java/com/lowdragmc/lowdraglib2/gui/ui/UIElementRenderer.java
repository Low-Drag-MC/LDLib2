package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;

public interface UIElementRenderer<T extends UIElement> {
    default void drawBackgroundAdditional(T element, IGUIContext context) {
        element.drawBackgroundAdditional(context);
    }

    default void drawBackgroundOverlay(T element, IGUIContext context) {
        element.drawBackgroundOverlay(context);
    }
}
