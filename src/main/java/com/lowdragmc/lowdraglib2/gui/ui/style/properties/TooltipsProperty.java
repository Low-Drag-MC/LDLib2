package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.TooltipsValue;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class TooltipsProperty extends Property<Tooltips> {
    public TooltipsProperty(String name, Tooltips initialValue) {
        super(name, Tooltips.class, Tooltips.CODEC, initialValue, TooltipsValue::new);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Tooltips> getter, Consumer<Tooltips> setter) {
        var arrayGroup = new ArrayConfiguratorGroup<>(name, true, () -> getter.get().asList(),
                (componentGetter, componentSetter) -> new StringConfigurator(name, () -> {
                    Component component = componentGetter.get();
                    return component.getString();
                }, s -> componentSetter.accept(Component.translatable(s)), "", true), true);
        arrayGroup.setAddDefault(Component::empty);
        arrayGroup.setOnUpdate(list -> setter.accept(Tooltips.of(list)));
        return arrayGroup;
    }
}
