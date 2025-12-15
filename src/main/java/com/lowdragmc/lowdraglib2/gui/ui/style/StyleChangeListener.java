package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.Nullable;

public interface StyleChangeListener<T> {
    void onComputedChange(UIElement element, Property<T> p, @Nullable T oldVal, @Nullable T newVal);
}