package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;

import java.util.Map;

public class Style {
    public final UIElement holder;

    public Style(UIElement holder) {
        this.holder = holder;
    }

    public void applyStyles(Map<String, StyleValue<?>> values) {
    }
}
