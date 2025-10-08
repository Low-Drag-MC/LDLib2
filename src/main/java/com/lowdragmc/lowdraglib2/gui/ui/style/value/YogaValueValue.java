package com.lowdragmc.lowdraglib2.gui.ui.style.value;

import org.appliedenergistics.yoga.YogaUnit;
import org.appliedenergistics.yoga.YogaValue;
import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class YogaValueValue extends StyleValue<YogaValue> {

    public YogaValueValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable YogaValue doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static YogaValue parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String s = rawValue.trim().toLowerCase(Locale.ROOT);

        switch (s) {
            case "auto":
                return YogaValue.AUTO;
            case "undefined":
                return YogaValue.UNDEFINED;
            case "fit-content":
                return new YogaValue(Float.NaN, YogaUnit.FIT_CONTENT);
            case "max-content":
                return new YogaValue(Float.NaN, YogaUnit.MAX_CONTENT);
            case "stretch":
                return new YogaValue(Float.NaN, YogaUnit.STRETCH);
        }

        if (s.endsWith("%")) {
            try {
                float value = Float.parseFloat(s.substring(0, s.length() - 1));
                return YogaValue.percent(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        try {
            float value = Float.parseFloat(s);
            return YogaValue.point(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
