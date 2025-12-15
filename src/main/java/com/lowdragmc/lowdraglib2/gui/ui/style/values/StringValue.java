package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;

public class StringValue extends StyleValue<String> {

    public StringValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected String doCompute(String rawValue) {
        return rawValue;
    }
    
}