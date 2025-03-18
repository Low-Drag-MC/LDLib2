package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;

public abstract class Ref implements IRef {
    @Getter
    protected final ManagedKey key;
    @Getter
    protected final IAccessor<?> accessor;
    @Getter
    @Setter
    private String persistedPrefixName;
    @Getter
    protected boolean isSyncDirty, isPersistedDirty;
    @Setter
    protected BooleanConsumer onSyncListener = changed -> {};
    @Setter
    protected BooleanConsumer onPersistedListener = changed -> {};

    protected Ref(ManagedKey key, IAccessor<?> accessor) {
        this.key = key;
        this.accessor = accessor;
    }

    @Override
    public void clearSyncDirty() {
        isSyncDirty = false;
        if (key.isDestSync()) {
            onSyncListener.accept(false);
        }
    }

    @Override
    public void clearPersistedDirty() {
        isPersistedDirty = false;
        if (key.isPersist()) {
            onPersistedListener.accept(false);
        }
    }

    @Override
    public void markAsDirty() {
        if (key.isDestSync()) {
            isSyncDirty = true;
            onSyncListener.accept(true);
        }
        if (key.isPersist()) {
            isPersistedDirty = true;
            onPersistedListener.accept(true);
        }
    }
}
