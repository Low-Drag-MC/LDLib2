package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.IDirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import lombok.Getter;

/**
 * DirectRef represents a reference to a value, the value is changeable (may not be the same instance or have internal changes).
 * Used the {@link IDirectVar} to visit and modify the value.
 */
public abstract class DirectRef<VAR extends IDirectVar<?>> extends Ref {
    @Getter
    protected final VAR field;

    protected DirectRef(VAR field, ManagedKey key, IDirectAccessor<VAR> accessor) {
        super(key, accessor);
        this.field = field;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IAccessor<IRef> getAccessor() {
        return (IAccessor<IRef>) super.getAccessor();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readRaw() {
        return (T) getField().value();
    }
}
