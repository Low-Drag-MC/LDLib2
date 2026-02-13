package com.lowdragmc.lowdraglib2.configurator.accessors;


import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.TextAreaConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import org.jetbrains.annotations.Nullable;

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
    public String[] defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return field.getAnnotation(DefaultValue.class).stringValue();
        }
        return new String[0];
    }

    @Override
    public Configurator create(String name, Supplier<String[]> supplier, Consumer<String[]> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new TextAreaConfigurator(name, supplier, consumer, defaultValue(field), forceUpdate);
    }
}
