package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.jetbrains.annotations.Nullable;

public class StyleSizeLengthValue extends StyleValue<StyleSizeLength> {

    public StyleSizeLengthValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable StyleSizeLength doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static StyleSizeLength parse(String rawValue) {
        rawValue = rawValue.trim();
        if (rawValue.equalsIgnoreCase("auto")) {
            return StyleSizeLength.ofAuto();
        } else if (rawValue.equalsIgnoreCase("undefined")) {
            return StyleSizeLength.undefined();
        } else if (rawValue.equalsIgnoreCase("max-content")) {
            return StyleSizeLength.ofMaxContent();
        } else if (rawValue.equalsIgnoreCase("fit-content")) {
            return StyleSizeLength.ofFitContent();
        } else if (rawValue.equalsIgnoreCase("stretch")) {
            return StyleSizeLength.ofStretch();
        }
        var yogaValue = YogaValueValue.parse(rawValue);
        return yogaValue == null ? null : StyleSizeLength.fromYogaValue(yogaValue);
    }
}
