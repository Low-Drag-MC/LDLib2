package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.FloatOptionalConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.IValueInterpolator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.FloatOptionalValue;
import lombok.experimental.Accessors;
import org.appliedenergistics.yoga.numeric.FloatOptional;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class FloatOptionalProperty extends Property<FloatOptional> {
    public FloatOptionalProperty(String name, FloatOptional initialValue) {
        super(name, FloatOptional.class, YogaCodecs.FLOAT_OPTIONAL_CODEC, initialValue, FloatOptionalValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<FloatOptional> getter, Consumer<FloatOptional> setter) {
        return new FloatOptionalConfigurator(name, getter, setter, initialValue, true);
    }

    private FloatOptional interpolate(FloatOptional from, FloatOptional to, float interpolation) {
        if (from.isDefined() && to.isDefined()) {
            return FloatOptional.of(from.getValue() + (to.getValue() - from.getValue()) * interpolation);
        }
        return IValueInterpolator.<FloatOptional>binary().interpolate(from, to, interpolation);
    }

}
