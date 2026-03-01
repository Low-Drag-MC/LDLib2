package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;

public class BoolValue extends StyleValue<Boolean> {

    public BoolValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected Boolean doCompute(String rawValue) {
        return Boolean.parseBoolean(rawValue.trim());
    }
    
}