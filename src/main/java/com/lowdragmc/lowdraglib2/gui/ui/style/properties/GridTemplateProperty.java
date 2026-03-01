package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridTemplateValue;
import lombok.experimental.Accessors;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class GridTemplateProperty extends Property<GridTemplate> {
    public GridTemplateProperty(String name, GridTemplate initialValue) {
        super(name, GridTemplate.class, TaffyCodecs.GRID_TEMPLATE_CODEC, initialValue, GridTemplateValue::new);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<GridTemplate> getter, Consumer<GridTemplate> setter) {
        var configurator = new StringConfigurator(
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
        ).setTextValidator(str -> GridTemplateValue.parse(str) != null);
        configurator.setSupplier(() -> {
            var current = configurator.getValue();
            var latest = GridTemplateValue.toString(getter.get());
            if (Objects.equals(current, latest) ||
                    Objects.equals(GridTemplateValue.parse(latest), GridTemplateValue.parse(current))) return current;
            return latest;
        });
        return configurator;
    }

}
