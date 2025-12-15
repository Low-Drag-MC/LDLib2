package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.jetbrains.annotations.Nullable;

public class FloatOptionalValue extends StyleValue<FloatOptional> {

    public FloatOptionalValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable FloatOptional doCompute(String rawValue) {
        rawValue = rawValue.trim();
        if (rawValue.equalsIgnoreCase("undefined")) {
            return FloatOptional.of();
        } else {
            return FloatOptional.of(Float.parseFloat(rawValue));
        }
    }
}
