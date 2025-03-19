package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.accessor.IDirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import lombok.Getter;

import javax.annotation.Nullable;

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
    public IDirectAccessor<VAR> getAccessor() {
        return (IDirectAccessor<VAR>) super.getAccessor();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T readRaw() {
        return (T) getField().value();
    }
}
