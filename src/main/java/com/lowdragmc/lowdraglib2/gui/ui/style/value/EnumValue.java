package com.lowdragmc.lowdraglib2.gui.ui.style.value;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleContext;

public class EnumValue<T extends Enum> extends StyleValue<T> {
    private final Class<Enum> type;

    public EnumValue(Class<Enum> type) {
        super(null, false);
        this.type = type;
    }

    @Override
    protected T doCompute(StyleContext context) {

        return null;
    }
}
