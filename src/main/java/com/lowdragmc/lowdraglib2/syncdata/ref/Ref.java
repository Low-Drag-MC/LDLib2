package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;

import org.jetbrains.annotations.Nullable;
import java.util.function.Predicate;

public abstract class Ref<TYPE> implements IRef<TYPE> {
    @Getter
    protected final ManagedKey key;
    @Getter
    protected final IAccessor<TYPE> accessor;
    @Getter
    @Setter
    private String persistedPrefixName;
    @Getter
    protected boolean isSyncDirty, isPersistedDirty;
    @Setter
    protected BooleanConsumer onSyncListener = changed -> {};
    @Setter
    protected BooleanConsumer onPersistedListener = changed -> {};
    @Setter
    @Nullable
    protected Predicate<TYPE> conditionalSynced;
    // a change found while the condition held the field back, to send once it no longer does
    private boolean heldBackChange;

    protected Ref(ManagedKey key, IAccessor<TYPE> accessor) {
        this.key = key;
        this.accessor = accessor;
    }

    @Override
    public void clearSyncDirty() {
        isSyncDirty = false;
        heldBackChange = false;
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
            if (!key.isLazy() && isHeldBack()) {
                heldBackChange = true;
            } else {
                markSyncDirty();
            }
        }
        if (key.isPersist()) {
            isPersistedDirty = true;
            onPersistedListener.accept(true);
        }
    }

    private void markSyncDirty() {
        isSyncDirty = true;
        onSyncListener.accept(true);
    }

    private boolean isHeldBack() {
        return conditionalSynced != null && !conditionalSynced.test(readRaw());
    }

    @Override
    public final void update() {
        if (isHeldBack()) {
            if (key.isPersist()) updateSync();
            return;
        }
        if (heldBackChange) {
            heldBackChange = false;
            markSyncDirty();
        }
        updateSync();
    }

    protected abstract void updateSync();
}
