package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StyleLengthConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.IValueInterpolator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.StyleLengthValue;
import lombok.experimental.Accessors;
import org.appliedenergistics.yoga.YogaValue;
import org.appliedenergistics.yoga.style.StyleLength;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class StyleLengthProperty extends Property<StyleLength> {
    public StyleLengthProperty(String name, StyleLength initialValue) {
        super(name, StyleLength.class, YogaCodecs.STYLE_LENGTH_CODEC, initialValue, StyleLengthValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<StyleLength> getter, Consumer<StyleLength> setter) {
        return new StyleLengthConfigurator(name, getter, setter, initialValue, true);
    }

    private StyleLength interpolate(StyleLength from, StyleLength to, float interpolation) {
        var f = from.asYogaValue();
        var t = to.asYogaValue();
        if (f.unit == t.unit) {
            return StyleLength.fromYogaValue(new YogaValue(f.value + (t.value - f.value) * interpolation, f.unit));
        }
        return IValueInterpolator.<StyleLength>binary().interpolate(from, to, interpolation);
    }
}
