package com.lowdragmc.lowdraglib2.configurator.accessors;


import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote ComponentAccessor
 */
@LDLRegisterClient(name = "component", registry = "ldlib2:configurator_accessor")
public class ComponentAccessor implements IConfiguratorAccessor<Component> {

    @Override
    public boolean test(Class<?> type) {
        return type == Component.class;
    }

    @Override
    public Component defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return Component.nullToEmpty(field.getAnnotation(DefaultValue.class).stringValue()[0]);
        }
        return Component.empty();
    }

    @Override
    public Configurator create(String name, Supplier<Component> supplier, Consumer<Component> consumer, boolean forceUpdate, Field field, Object owner) {
        return new StringConfigurator(name, () -> {
            Component component = supplier.get();
            return component.getString();
        }, s -> consumer.accept(Component.translatable(s)), "", forceUpdate);
    }
}
