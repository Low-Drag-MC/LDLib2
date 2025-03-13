package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;

import java.lang.ref.WeakReference;

public class ReadonlyRef implements IRef {
    @Getter
    @Setter
    private String persistedPrefixName;
    private ManagedKey key;
    @Getter
    private boolean isSyncDirty, isPersistedDirty;
    @Setter
    protected BooleanConsumer onSyncListener = changed -> {
    };
    @Setter
    protected BooleanConsumer onPersistedListener = changed -> {
    };

    protected final WeakReference<?> reference;

    public ReadonlyRef(Object value) {
        this.reference = new WeakReference<>(value);
        init();
    }

    protected void init() {
        if (!getKey().isLazy()) {
            if (getReference().get() instanceof IContentChangeAware handler) {
                replaceHandler(handler);
            } else if (readRaw() instanceof IManaged) {

            }
            else {
                throw new IllegalArgumentException("complex sync field must be an IContentChangeAware if not lazy!");
            }
        }
    }

    protected void replaceHandler(IContentChangeAware handler) {
        var onContentChanged = handler.getOnContentsChanged();
        if (onContentChanged != null) {
            handler.setOnContentsChanged(() -> {
                markAsDirty();
                onContentChanged.run();
            });
        } else {
            handler.setOnContentsChanged(() -> markAsDirty());
        }
    }

    @Override
    public ManagedKey getKey() {
        return key;
    }

    public IRef setKey(ManagedKey key) {
        this.key = key;
        return this;
    }

    public WeakReference<?> getReference() {
        return reference;
    }

    @Override
    public void clearSyncDirty() {
        isSyncDirty = false;
        if (readRaw() instanceof IManaged managed) {
            for (var field : managed.getSyncStorage().getSyncFields()) {
                field.clearSyncDirty();
            }
        }
        if (key.isDestSync()) {
            onSyncListener.accept(false);
        }
    }

    @Override
    public void clearPersistedDirty() {
        isPersistedDirty = false;
        if (readRaw() instanceof IManaged managed) {
            for (var field : managed.getSyncStorage().getPersistedFields()) {
                field.clearPersistedDirty();
            }
        }
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

    @Override
    public void update() {
        if (readRaw() instanceof IManaged managed) {
            var storage = managed.getSyncStorage();

            for (IRef field : storage.getNonLazyFields()) {
                field.update();
            }

            if (storage.hasDirtySyncFields()) {
                if (key.isDestSync()) {
                    markAsDirty();
                } else {
                    for (var field : storage.getSyncFields()) {
                        field.clearSyncDirty();
                    }
                }
            }

            if (storage.hasDirtyPersistedFields()) {
                if (key.isPersist()) {
                    markAsDirty();
                } else {
                    for (var field : storage.getPersistedFields()) {
                        field.clearPersistedDirty();
                    }
                }
            }
        }
    }

    @Override
    public boolean isLazy() {
        return lazy;
    }


    @Override
    public <T> T readRaw() {
        //noinspection unchecked
        return (T) this.reference.get();
    }
}
