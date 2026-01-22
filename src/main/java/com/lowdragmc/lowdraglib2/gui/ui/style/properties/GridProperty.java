package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.Grid;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridValue;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Property for CSS grid-row and grid-column.
 * Represents grid item placement using start and end lines.
 */
@Accessors(chain = true)
public class GridProperty extends Property<Grid> {
    public GridProperty(String name, Grid initialValue) {
        super(name, Grid.class, TaffyCodecs.GRID_CODEC, initialValue, GridValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Grid> getter, Consumer<Grid> setter) {
        return new StringConfigurator(
                name,
                () -> GridValue.toString(getter.get()),
                str -> {
                    Grid parsed = GridValue.parse(str);
                    if (parsed != null) {
                        setter.accept(parsed);
                    }
                },
                "auto",
                true
        );
    }

    private Grid interpolate(Grid from, Grid to, float interpolation) {
        // Binary snap: use 'from' until halfway, then switch to 'to'
        // Grid placement is discrete and doesn't interpolate smoothly
        return interpolation < 0.5f ? from : to;
    }
}
