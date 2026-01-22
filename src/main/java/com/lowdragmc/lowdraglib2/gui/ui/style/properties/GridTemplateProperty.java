package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridTemplateValue;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class GridTemplateProperty extends Property<GridTemplate> {
    public GridTemplateProperty(String name, GridTemplate initialValue) {
        super(name, GridTemplate.class, TaffyCodecs.GRID_TEMPLATE_CODEC, initialValue, GridTemplateValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<GridTemplate> getter, Consumer<GridTemplate> setter) {
        return new StringConfigurator(
                name,
                () -> GridTemplateValue.toString(getter.get()),
                str -> {
                    GridTemplate parsed = GridTemplateValue.parse(str);
                    if (parsed != null) {
                        setter.accept(parsed);
                    }
                },
                "",
                true
        );
    }

    private GridTemplate interpolate(GridTemplate from, GridTemplate to, float interpolation) {
        // Binary snap: use 'from' until halfway, then switch to 'to'
        // Grid templates have discrete structure that doesn't interpolate smoothly
        return interpolation < 0.5f ? from : to;
    }
}
