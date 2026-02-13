package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridAuto;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridAutoValue;
import lombok.experimental.Accessors;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class GridAutoProperty extends Property<GridAuto> {
    public GridAutoProperty(String name, GridAuto initialValue) {
        super(name, GridAuto.class, TaffyCodecs.GRID_AUTO_CODEC, initialValue, GridAutoValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<GridAuto> getter, Consumer<GridAuto> setter) {
        var configurator = new StringConfigurator(
                name,
                () -> GridAutoValue.toString(getter.get()),
                str -> {
                    GridAuto parsed = GridAutoValue.parse(str);
                    if (parsed != null) {
                        setter.accept(parsed);
                    }
                },
                "",
                true
        ).setTextValidator(str -> GridAutoValue.parse(str) != null);
        configurator.setSupplier(() -> {
            var current = configurator.getValue();
            var latest = GridAutoValue.toString(getter.get());
            if (Objects.equals(current, latest) ||
                    Objects.equals(GridAutoValue.parse(latest), GridAutoValue.parse(current))) return current;
            return latest;
        });
        return configurator;
    }

    private GridAuto interpolate(GridAuto from, GridAuto to, float interpolation) {
        // Binary snap: use 'from' until halfway, then switch to 'to'
        // Grid auto values have discrete structure that doesn't interpolate smoothly
        return interpolation < 0.5f ? from : to;
    }
}
