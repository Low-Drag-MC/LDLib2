package com.lowdragmc.lowdraglib.syncdata.ref;

import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.var.IVar;
import lombok.Getter;

/**
 * DirectRef represents a reference to a value, the value is changeable (may not be the same instance or have internal changes).
 * Used the {@link IVar} to visit and modify the value.
 */
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
}
