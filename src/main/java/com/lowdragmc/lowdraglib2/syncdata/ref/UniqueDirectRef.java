package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.var.IVar;

import java.util.Objects;
public class UniqueDirectRef<TYPE> extends DirectRef<TYPE> {
    protected TYPE oldValue;

    public UniqueDirectRef(IVar<TYPE> field, ManagedKey key, IAccessor<TYPE> accessor) {
        super(field, key, accessor);
        oldValue = readRaw();
    }

    @Override
    protected void updateSync() {
        TYPE newValue = readRaw();
        if (!Objects.equals(oldValue, newValue)) {
            oldValue = newValue;
            markAsDirty();
        }
    }
}
