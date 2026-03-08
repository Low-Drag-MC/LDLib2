package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRenderer;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;

public interface RegisteredUIElementRenderer<T extends UIElement, S extends RegisteredUIElementRenderer<T, S>> extends UIElementRenderer<T>, ILDLRegisterClient<S, S> {
    Class<T> type();
}
