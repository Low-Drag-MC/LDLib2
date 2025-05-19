package com.lowdragmc.lowdraglib.configurator.accessors;

import com.lowdragmc.lowdraglib.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib.registry.ILDLRegisterClient;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote IConfiguratorAccessor
 */
public interface IConfiguratorAccessor<T> extends ILDLRegisterClient<IConfiguratorAccessor<?>, IConfiguratorAccessor<?>> {
    IConfiguratorAccessor<?> DEFAULT = type -> true;

    /**
     * @param type the class type
     * @return true if this accessor can handle the given type
     */
    boolean test(Class<?> type);

    default T defaultValue(Field field, Class<?> type) {
        return null;
    }

    /**
     * @param name the name of the configurator
     * @param supplier the supplier for the value
     * @param consumer the consumer for the value
     * @param forceUpdate whether to force update the configurator
     * @param field the field to be configured
     * @return a new configurator instance
     */
    default Configurator create(String name, Supplier<T> supplier, Consumer<T> consumer, boolean forceUpdate, Field field) {
        return new Configurator(name);
    }
}
