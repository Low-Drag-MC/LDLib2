package com.lowdragmc.lowdraglib2.syncdata;

import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib2.syncdata.storage.IFieldUpdateListener;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;

public interface IManaged {

    /**
     * Get the sync field holder, usually a static field.
     */
    default ManagedFieldHolder getFieldHolder() {
        return ManagedFieldHolder.ofCache(getClass());
    }

    /**
     * Get managed storage.
     */
    IManagedStorage getSyncStorage();

    /**
     * on field updated. Be aware of that it's not thread safe which may be called in any thread.
     */
    default void onSyncMarkChanged(IRef<?> ref, boolean isDirty) {
    }

    /**
     * on field updated.
     * Be aware of that it's not thread safe which may be called in any thread.
     */
    default void onPersistedMarkChanged(IRef<?> ref, boolean isDirty) {
        if (isDirty) {
            notifyPersistence();
            ref.clearPersistedDirty();
        }
    }

    /**
     * notify persisted. it may be called in any thread
     */
    void notifyPersistence();

    /**
     * add a listener to field update
     *
     * @param <T>      field type;
     * @param name     managed key
     * @param listener listener
     * @return callback that you can unsubscribe
     */
    default <T> ISubscription addSyncUpdateListener(String name, IFieldUpdateListener<T> listener) {
        return getSyncStorage().addSyncUpdateListener(getFieldHolder().getSyncedFieldIndex(name), listener);
    }

    /**
     * Marks a field as changed, so it will be synced.
     *
     * @param name the key of the field, usually its name
     */
    default void markDirty(String name) {
        getSyncStorage().getFieldByKey(getFieldHolder().getSyncedFieldIndex(name)).markAsDirty();
    }

}
