package com.lowdragmc.lowdraglib2.configurator.accessors;


import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.TextAreaConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "string_array", registry = "ldlib2:configurator_accessor")
public class StringArrayAccessor implements IConfiguratorAccessor<String[]> {

    @Override
    public boolean test(Class<?> type) {
        return type == String[].class;
    }

    @Override
    public String[] defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return field.getAnnotation(DefaultValue.class).stringValue();
        }
        return new String[0];
    }

    @Override
    public Configurator create(String name, Supplier<String[]> supplier, Consumer<String[]> consumer, boolean forceUpdate, Field field, Object owner) {
        return new TextAreaConfigurator(name, supplier, consumer, defaultValue(field, field.getType()), forceUpdate);
    }
}
