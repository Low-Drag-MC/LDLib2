package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.BooleanConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote NumberAccessor
 */
@LDLRegisterClient(name = "boolean", registry = "ldlib2:configurator_accessor")
public class BooleanAccessor extends TypesAccessor<Boolean> {

    public BooleanAccessor() {
        super(Boolean.class, boolean.class);
    }

    @Override
    public Boolean defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return field.getAnnotation(DefaultValue.class).booleanValue()[0];
        }
        return false;
    }

    @Override
    public Configurator create(String name, Supplier<Boolean> supplier, Consumer<Boolean> consumer, boolean forceUpdate, Field field, Object owner) {
        return new BooleanConfigurator(name, supplier, consumer, defaultValue(field, boolean.class), forceUpdate);
    }
}
