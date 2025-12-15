package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;

public class ColorValue extends StyleValue<Integer> {

    public ColorValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected Integer doCompute(String rawValue) {
        return ColorUtils.parseColor(rawValue);
    }
}