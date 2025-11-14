package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.ValueParser;

public class EnumValue<T extends Enum<T>> extends StyleValue<T> {
    private final Class<T> clazz;

    public EnumValue(Class<T> clazz, String rawValue) {
        super(rawValue);
        this.clazz = clazz;
    }

    public static <T extends Enum<T>> ValueParser<T> of(Class<T> clazz) {
        return raw -> new EnumValue<>(clazz, raw);
    }

    @Override
    protected T doCompute(String rawValue) {
        var constants = clazz.getEnumConstants();
        for (var constant : constants) {
            if (constant.name().equalsIgnoreCase(rawValue)) {
                return constant;
            }
        }
        return null;
    }
}
