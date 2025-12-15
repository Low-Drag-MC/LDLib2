package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StyleLengthConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.StyleLengthValue;
import lombok.experimental.Accessors;
import org.appliedenergistics.yoga.style.StyleLength;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class StyleLengthProperty extends Property<StyleLength> {
    public StyleLengthProperty(String name, StyleLength initialValue) {
        super(name, StyleLength.class, YogaCodecs.STYLE_LENGTH_CODEC, initialValue, StyleLengthValue::new);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<StyleLength> getter, Consumer<StyleLength> setter) {
        return new StyleLengthConfigurator(name, getter, setter, initialValue, true);
    }
}
