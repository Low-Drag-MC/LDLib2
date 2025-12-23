package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.IntValue;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class IntProperty extends Property<Integer> {
    @Setter
    private int min = Integer.MIN_VALUE;
    @Setter
    private int max = Integer.MAX_VALUE;
    @Getter @Setter
    private int step = 1;

    public IntProperty(String name, int initialValue) {
        super(name, Integer.class, Codec.INT, initialValue, IntValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    public IntProperty setRange(int min, int max) {
        return setMin(min).setMax(max);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Integer> getter, Consumer<Integer> setter) {
        return new NumberConfigurator(name, getter::get, number -> setter.accept(number.intValue()), initialValue, true)
                .setType(ConfigNumber.Type.INTEGER)
                .setRange(min, max)
                .setWheel(step);
    }

    private int interpolate(int from, int to, float interpolation) {
        return Math.round(from + (to - from) * interpolation);
    }
}
