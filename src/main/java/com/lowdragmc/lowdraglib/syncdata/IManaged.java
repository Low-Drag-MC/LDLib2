package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.storage.IFieldUpdateListener;
import com.lowdragmc.lowdraglib.syncdata.storage.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.ref.IRef;

public interface IManaged {

    /**
     * Get the sync field holder, usually a static field.
     */
    ManagedFieldHolder getFieldHolder();

    /**
     * Get managed storage.
     */
    IManagedStorage getSyncStorage();

    /**
     * on field updated.
     * it may be called in any thread if you are using {@link com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity}.
     */
    default void onSyncMarkChanged(IRef ref, boolean isDirty) {
    }

    /**
     * on field updated.
     * it may be called in any thread if you are using {@link com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity}.
     */
    default void onPersistedMarkChanged(IRef ref, boolean isDirty) {
        if (isDirty) {
            onChanged();
            ref.clearPersistedDirty();
        }
    }

    /**
     * notify persisted. it may be called in any thread
     */
    void onChanged();

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
