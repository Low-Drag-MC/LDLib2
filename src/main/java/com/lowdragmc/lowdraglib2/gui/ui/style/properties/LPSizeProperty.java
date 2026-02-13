package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.LPSize;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.LPSizeValue;
import dev.vfyjxf.taffy.style.LengthPercentage;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Property for LPSize values (gap shorthand properties).
 */
@Accessors(chain = true)
public class LPSizeProperty extends Property<LPSize> {
    public LPSizeProperty(String name, LPSize initialValue) {
        super(name, LPSize.class, TaffyCodecs.LP_SIZE_CODEC, initialValue, LPSizeValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<LPSize> getter, Consumer<LPSize> setter) {
        // Use StringConfigurator for CSS-style editing
        return new StringConfigurator(
                name,
                () -> LPSizeValue.toString(getter.get()),
                str -> {
                    LPSize parsed = LPSizeValue.parse(str);
                    if (parsed != null) {
                        setter.accept(parsed);
                    }
                },
                "0",
                true
        );
    }

    private LPSize interpolate(LPSize from, LPSize to, float interpolation) {
        // Interpolate width and height independently
        LengthPercentage width = interpolateLengthPercentage(from.size().width, to.size().width, interpolation);
        LengthPercentage height = interpolateLengthPercentage(from.size().height, to.size().height, interpolation);

        return new LPSize(new dev.vfyjxf.taffy.geometry.TaffySize<>(width, height));
    }

    private LengthPercentage interpolateLengthPercentage(LengthPercentage from, LengthPercentage to, float interpolation) {
        // Only interpolate if both values are the same numeric type
        if (from.isLength() && to.isLength()) {
            float interpolated = from.getValue() + (to.getValue() - from.getValue()) * interpolation;
            return LengthPercentage.length(interpolated);
        } else if (from.isPercent() && to.isPercent()) {
            float interpolated = from.getValue() + (to.getValue() - from.getValue()) * interpolation;
            return LengthPercentage.percent(interpolated);
        }

        // For different types, use binary snap
        return interpolation < 0.5f ? from : to;
    }
}
