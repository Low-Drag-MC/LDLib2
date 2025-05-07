package com.lowdragmc.lowdraglib.gui.ui.style.value;

import com.lowdragmc.lowdraglib.gui.ui.style.StyleContext;

public abstract class StyleValue<T> {
    protected final String rawValue;
    protected T computedValue;
    private final boolean inheritable;

    protected StyleValue(String rawValue, boolean inheritable) {
        this.rawValue = rawValue;
        this.inheritable = inheritable;
    }

    public T compute(StyleContext context) {
        if (computedValue == null) {
            computedValue = doCompute(context);
        }
        return computedValue;
    }
    
    protected abstract T doCompute(StyleContext context);
}