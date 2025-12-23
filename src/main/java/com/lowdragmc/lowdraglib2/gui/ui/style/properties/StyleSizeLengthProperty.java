package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StyleSizeLengthConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.IValueInterpolator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.StyleSizeLengthValue;
import lombok.experimental.Accessors;
import org.appliedenergistics.yoga.YogaValue;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class StyleSizeLengthProperty extends Property<StyleSizeLength> {
    public StyleSizeLengthProperty(String name, StyleSizeLength initialValue) {
        super(name, StyleSizeLength.class, YogaCodecs.STYLE_SIZE_LENGTH_CODEC, initialValue, StyleSizeLengthValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<StyleSizeLength> getter, Consumer<StyleSizeLength> setter) {
        return new StyleSizeLengthConfigurator(name, getter, setter, initialValue, true);
    }

    private StyleSizeLength interpolate(StyleSizeLength from, StyleSizeLength to, float interpolation) {
        var f = from.asYogaValue();
        var t = to.asYogaValue();
        if (f.unit == t.unit) {
            return StyleSizeLength.fromYogaValue(new YogaValue(f.value + (t.value - f.value) * interpolation, f.unit));
        }
        return IValueInterpolator.<StyleSizeLength>binary().interpolate(from, to, interpolation);
    }
}
