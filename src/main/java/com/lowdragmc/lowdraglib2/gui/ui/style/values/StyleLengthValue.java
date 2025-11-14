package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import org.appliedenergistics.yoga.style.StyleLength;
import org.jetbrains.annotations.Nullable;

public class StyleLengthValue extends StyleValue<StyleLength> {

    public StyleLengthValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable StyleLength doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static StyleLength parse(String rawValue) {
        rawValue = rawValue.trim();
        if (rawValue.equalsIgnoreCase("auto")) {
            return StyleLength.ofAuto();
        } else if (rawValue.equalsIgnoreCase("undefined")) {
            return StyleLength.undefined();
        }
        var yogaValue = YogaValueValue.parse(rawValue);
        return yogaValue == null ? null : StyleLength.fromYogaValue(yogaValue);
    }
}
