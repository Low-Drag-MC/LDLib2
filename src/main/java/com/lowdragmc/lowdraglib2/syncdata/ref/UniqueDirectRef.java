package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.var.IVar;

import java.util.Objects;

/**
 * UniqueDirectRef represents a reference to a unique value, which is updated only when the instance value changed.
 * {@link Objects#equals(Object, Object)} is used to compare the value.
 */
public class UniqueDirectRef<TYPE> extends DirectRef<TYPE> {
    protected TYPE oldValue;

    public UniqueDirectRef(IVar<TYPE> field, ManagedKey key, IAccessor<TYPE> accessor) {
        super(field, key, accessor);
        oldValue = readRaw();
    }

    @Override
    public void update() {
        TYPE newValue = readRaw();
        if (!Objects.equals(oldValue, newValue)) {
            oldValue = newValue;
            markAsDirty();
        }
    }
}
