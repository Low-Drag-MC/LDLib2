package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;

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

    /**
     * Retrieves the default value for a given field and type. Make sure the return value is a new instance, which will also be used by {@link ArrayConfiguratorAccessor}
     *
     * @param field the field for which the default value is being retrieved
     * @param type the class type of the field
     * @return the default value of the specified type, or null if no default value is specified
     */
    default T defaultValue(Field field, Class<?> type) {
        return null;
    }

    /**
     * @param name the name of the configurator
     * @param supplier the supplier for the value
     * @param consumer the consumer for the value
     * @param forceUpdate whether to force update the configurator
     * @param field the field to be configured
     * @param owner the field owner
     * @return a new configurator instance
     */
    default Configurator create(String name, Supplier<T> supplier, Consumer<T> consumer, boolean forceUpdate, Field field, Object owner) {
        return new Configurator(name);
    }
}
