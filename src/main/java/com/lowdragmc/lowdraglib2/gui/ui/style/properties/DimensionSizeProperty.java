package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.DimensionSize;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.DimensionSizeValue;
import dev.vfyjxf.taffy.style.TaffyDimension;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Property for DimensionSize values (size, min/max-size shorthand properties).
 */
@Accessors(chain = true)
public class DimensionSizeProperty extends Property<DimensionSize> {
    public DimensionSizeProperty(String name, DimensionSize initialValue) {
        super(name, DimensionSize.class, TaffyCodecs.DIMENSION_SIZE_CODEC, initialValue, DimensionSizeValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<DimensionSize> getter, Consumer<DimensionSize> setter) {
        // Use StringConfigurator for CSS-style editing
        return new StringConfigurator(
                name,
                () -> DimensionSizeValue.toString(getter.get()),
                str -> {
                    DimensionSize parsed = DimensionSizeValue.parse(str);
                    if (parsed != null) {
                        setter.accept(parsed);
                    }
                },
                "auto",
                true
        );
    }

    private DimensionSize interpolate(DimensionSize from, DimensionSize to, float interpolation) {
        // Interpolate width and height independently
        TaffyDimension width = interpolateDimension(from.size().width, to.size().width, interpolation);
        TaffyDimension height = interpolateDimension(from.size().height, to.size().height, interpolation);

        return new DimensionSize(new dev.vfyjxf.taffy.geometry.TaffySize<>(width, height));
    }

    private TaffyDimension interpolateDimension(TaffyDimension from, TaffyDimension to, float interpolation) {
        // Only interpolate if both values are the same numeric type
        if (from.isLength() && to.isLength()) {
            float interpolated = from.getValue() + (to.getValue() - from.getValue()) * interpolation;
            return TaffyDimension.length(interpolated);
        } else if (from.isPercent() && to.isPercent()) {
            float interpolated = from.getValue() + (to.getValue() - from.getValue()) * interpolation;
            return TaffyDimension.percent(interpolated);
        }

        // For different types or auto, use binary snap
        return interpolation < 0.5f ? from : to;
    }
}
