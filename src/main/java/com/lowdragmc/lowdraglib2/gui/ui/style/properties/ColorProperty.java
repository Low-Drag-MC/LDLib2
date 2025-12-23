package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.ColorValue;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.serialization.Codec;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class ColorProperty extends Property<Integer> {
    public ColorProperty(String name, int initialValue) {
        super(name, Integer.class, Codec.INT, initialValue, ColorValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Integer> getter, Consumer<Integer> setter) {
        return new ColorConfigurator(name, getter, setter, initialValue, true);
    }

    private int interpolate(int from, int to, float interpolation) {
        return ColorUtils.blendOklabColor(from, to, interpolation);
    }
}
