package com.lowdragmc.lowdraglib2.editor_outdated.accessors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote TypesAccessor
 */
public abstract class TypesAccessor<T> implements IConfiguratorAccessor<T> {
    public Set<Class<?>> types = new HashSet<>();

    public TypesAccessor(Class<?>... types) {
        this.types.addAll(List.of(types));
    }

    @Override
    public boolean test(Class<?> type) {
        return types.contains(type);
    }
}
