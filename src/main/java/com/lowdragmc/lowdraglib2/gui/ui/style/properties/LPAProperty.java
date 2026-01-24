package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.LPAConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.LPAValue;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Property for LengthPercentageAuto values (individual margin/padding properties).
 */
@Accessors(chain = true)
public class LPAProperty extends Property<LengthPercentageAuto> {
    public LPAProperty(String name, LengthPercentageAuto initialValue) {
        super(name, LengthPercentageAuto.class, TaffyCodecs.LPA_STYLE_LENGTH_COMPAT_CODEC, initialValue, LPAValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<LengthPercentageAuto> getter, Consumer<LengthPercentageAuto> setter) {
        return new LPAConfigurator(
                name,
                getter,
                setter,
                initialValue,
                true
        );
    }

    private LengthPercentageAuto interpolate(LengthPercentageAuto from, LengthPercentageAuto to, float interpolation) {
        // Only interpolate if both values are the same numeric type (LENGTH or PERCENT)
        if (from.getType() == to.getType()) {
            if (from.isLength()) {
                // Interpolate lengths
                float interpolated = from.getValue() + (to.getValue() - from.getValue()) * interpolation;
                return LengthPercentageAuto.length(interpolated);
            } else if (from.isPercent()) {
                // Interpolate percentages
                float interpolated = from.getValue() + (to.getValue() - from.getValue()) * interpolation;
                return LengthPercentageAuto.percent(interpolated);
            }
        }

        // For different types or non-numeric types, use binary snap
        return interpolation < 0.5f ? from : to;
    }
}
