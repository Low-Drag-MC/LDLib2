package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;

public class IntValue extends StyleValue<Integer> {

    public IntValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected Integer doCompute(String rawValue) {
        return Integer.parseInt(rawValue.trim());
    }
    
}