package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.Grid;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridValue;
import lombok.experimental.Accessors;

import java.util.Objects;
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
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Grid> getter, Consumer<Grid> setter) {
        var configurator = new StringConfigurator(
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
        ).setTextValidator(str -> GridValue.parse(str) != null);
        configurator.setSupplier(() -> {
            var current = configurator.getValue();
            var latest = GridValue.toString(getter.get());
            if (Objects.equals(current, latest) ||
                    Objects.equals(GridValue.parse(latest), GridValue.parse(current))) return current;
            return latest;
        });
        return configurator;
    }

}
