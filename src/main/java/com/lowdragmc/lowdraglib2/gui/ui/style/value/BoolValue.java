package com.lowdragmc.lowdraglib2.gui.ui.style.value;

public class BoolValue extends StyleValue<Boolean> {

    public BoolValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected Boolean doCompute(String rawValue) {
        return Boolean.parseBoolean(rawValue);
    }
    
}