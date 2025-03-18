package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.accessor.IDirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;

import java.util.Objects;

/**
 * UniqueDirectRef represents a reference to a unique value, which is updated only when the instance value changed.
 * {@link Objects#equals(Object, Object)} is used to compare the value.
 */
public final class UniqueDirectRef<T> extends DirectRef<IDirectVar<T>> {
    private T oldValue;

    public UniqueDirectRef(IDirectVar<T> field, ManagedKey key, IDirectAccessor<IDirectVar<T>> accessor) {
        super(field, key, accessor);
        oldValue = readRaw();
    }

    @Override
    public void update() {
        T newValue = readRaw();
        if (!Objects.equals(oldValue, newValue)) {
            oldValue = newValue;
            markAsDirty();
        }
    }
}
