package com.lowdragmc.lowdraglib2.editor_outdated.accessors;

import com.lowdragmc.lowdraglib2.editor_outdated.configurator.Configurator;
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

    boolean test(Class<?> type);

    default T defaultValue(Field field, Class<?> type) {
        return null;
    }

    default Configurator create(String name, Supplier<T> supplier, Consumer<T> consumer, boolean forceUpdate, Field field) {
        return new Configurator(name);
    }
}
