package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.DimensionConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.DimensionValue;
import dev.vfyjxf.taffy.style.TaffyDimension;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Property for TaffyDimension values (individual size, min/max-size properties).
 */
@Accessors(chain = true)
public class DimensionProperty extends Property<TaffyDimension> {
    public DimensionProperty(String name, TaffyDimension initialValue) {
        super(name, TaffyDimension.class, TaffyCodecs.DIMENSION_STYLE_SIZE_LENGTH_COMPAT_CODEC, initialValue, DimensionValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<TaffyDimension> getter, Consumer<TaffyDimension> setter) {
        return new DimensionConfigurator(
                name,
                getter,
                setter,
                initialValue,
                true
        );
    }

    private TaffyDimension interpolate(TaffyDimension from, TaffyDimension to, float interpolation) {
        // Only interpolate if both values are the same numeric type (LENGTH or PERCENT)
        if (from.isLength() && to.isLength()) {
            // Interpolate lengths
            float interpolated = from.getValue() + (to.getValue() - from.getValue()) * interpolation;
            return TaffyDimension.length(interpolated);
        } else if (from.isPercent() && to.isPercent()) {
            // Interpolate percentages
            float interpolated = from.getValue() + (to.getValue() - from.getValue()) * interpolation;
            return TaffyDimension.percent(interpolated);
        }

        // For different types or auto, use binary snap
        return interpolation < 0.5f ? from : to;
    }
}
