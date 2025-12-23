package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.style.IValueInterpolator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.FloatValue;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.appliedenergistics.yoga.numeric.FloatOptional;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class FloatProperty extends Property<Float> {
    @Setter
    private float min = -Float.MAX_VALUE;
    @Setter
    private float max = Float.MAX_VALUE;
    @Getter @Setter
    private float step = 0.1f;

    public FloatProperty(String name, float initialValue) {
        super(name, Float.class, Codec.FLOAT, initialValue, FloatValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    public FloatProperty setRange(float min, float max) {
        return setMin(min).setMax(max);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Float> getter, Consumer<Float> setter) {
        return new NumberConfigurator(name, getter::get, number -> setter.accept(number.floatValue()), initialValue, true)
                .setType(ConfigNumber.Type.FLOAT)
                .setRange(min, max)
                .setWheel(step);
    }

    private float interpolate(float from, float to, float interpolation) {
        return from + (to - from) * interpolation;
    }
}
