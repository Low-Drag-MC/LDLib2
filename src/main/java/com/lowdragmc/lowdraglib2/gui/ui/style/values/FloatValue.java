package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;

public class FloatValue extends StyleValue<Float> {

    public FloatValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected Float doCompute(String rawValue) {
        return Float.parseFloat(rawValue.trim());
    }
    
}