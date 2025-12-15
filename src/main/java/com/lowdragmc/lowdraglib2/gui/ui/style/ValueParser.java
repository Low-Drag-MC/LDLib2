package com.lowdragmc.lowdraglib2.gui.ui.style;

public interface ValueParser<T> {
    StyleValue<T> parse(String rawValue);
}
