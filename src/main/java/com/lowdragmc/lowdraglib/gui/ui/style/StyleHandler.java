package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;

import javax.annotation.Nullable;

/**
 * The style handler is used to handle the style of the UI elements.
 */
public interface StyleHandler<VALUE extends StyleValue<?>> {
    /**
     * Called when the style of the element is applied or removed.
     */
    void onStyleChange(UIElement element, @Nullable VALUE value);
}
