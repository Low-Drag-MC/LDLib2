package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;

public class DoubleValue extends StyleValue<Double> {

    public DoubleValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected Double doCompute(String rawValue) {
        return Double.parseDouble(rawValue.trim());
    }
    
}