package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplateAreas;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridTemplateAreasValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridTemplateValue;
import lombok.experimental.Accessors;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class GridTemplateAreasProperty extends Property<GridTemplateAreas> {
    public GridTemplateAreasProperty(String name, GridTemplateAreas initialValue) {
        super(name, GridTemplateAreas.class, TaffyCodecs.GRID_TEMPLATE_AREAS_CODEC, initialValue, GridTemplateAreasValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<GridTemplateAreas> getter, Consumer<GridTemplateAreas> setter) {
        var configurator = new StringConfigurator(
                name,
                () -> GridTemplateAreasValue.toString(getter.get()),
                str -> {
                    GridTemplateAreas parsed = GridTemplateAreasValue.parse(str);
                    if (parsed != null) {
                        setter.accept(parsed);
                    }
                },
                "",
                true
        ).setTextValidator(str -> GridTemplateAreasValue.parse(str) != null);
        configurator.setSupplier(() -> {
            var current = configurator.getValue();
            var latest = GridTemplateAreasValue.toString(getter.get());
            if (Objects.equals(current, latest) ||
                    Objects.equals(GridTemplateAreasValue.parse(latest), GridTemplateAreasValue.parse(current))) return current;
            return latest;
        });
        return configurator;
    }

    private GridTemplateAreas interpolate(GridTemplateAreas from, GridTemplateAreas to, float interpolation) {
        // Binary snap: use 'from' until halfway, then switch to 'to'
        // Grid template areas have discrete structure that doesn't interpolate smoothly
        return interpolation < 0.5f ? from : to;
    }
}
