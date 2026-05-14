package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.AutoFloatConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.FloatValue;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class AutoFloatProperty extends Property<Float> {
    @Setter
    private float min = -Float.MAX_VALUE;
    @Setter
    private float max = Float.MAX_VALUE;
    @Getter @Setter
    private float step = 0.1f;

    public AutoFloatProperty(String name, float initialValue) {
        super(name, Float.class, Codec.FLOAT, initialValue, FloatValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    public AutoFloatProperty setRange(float min, float max) {
        return setMin(min).setMax(max);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Float> getter, Consumer<Float> setter) {
        return new AutoFloatConfigurator(name, getter, setter, initialValue, true)
                .setRange(min, max)
                .setWheel(step);
    }

    private float interpolate(float from, float to, float interpolation) {
        if (Float.isNaN(from) || Float.isNaN(to)) {
            return interpolation < 0.5f ? from : to;
        }
        return from + (to - from) * interpolation;
    }
}
