package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.var.IVar;
import lombok.Getter;
public abstract class DirectRef<TYPE> extends Ref<TYPE> {
    @Getter
    protected final IVar<TYPE> field;

    protected DirectRef(IVar<TYPE> field, ManagedKey key, IAccessor<TYPE> accessor) {
        super(key, accessor);
        this.field = field;
    }

    @Override
    public TYPE readRaw() {
        return field.value();
    }

    @Override
    public void writeRaw(TYPE value) {
        field.set(value);
    }
}
