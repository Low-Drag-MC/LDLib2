package com.lowdragmc.lowdraglib.syncdata.managed;

import com.google.common.base.Strings;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

import javax.annotation.Nullable;

/**
 * Ref is a reference to a field instance, it's used to detect / manage the field's dirty status.
 * <br>
 * Also, can be used to obtain the internal value.
 */
public interface IRef {
    /**
     * ManagedKey refer to ref's meta info.
     */
    ManagedKey getKey();

    /**
     * whether the ref is dirty and need to be synced.
     */
    boolean isSyncDirty();

    /**
     * whether the ref is dirty and need to be persisted.
     */
    boolean isPersistedDirty();

    /**
     * Clear sync dirty mark. It should be called after the field has been synced.
     */
    void clearSyncDirty();

    /**
     * Clear persisted dirty mark. It should be called after the field has been persisted.
     */
    void clearPersistedDirty();

    /**
     * Mark the ref as dirty, it should be called while the field has been changed.
     */
    void markAsDirty();

    /**
     * Called automatically if it is a non-lazy ref.
     * <br>
     * Implement this method to check its internal changed. If it has changed, it should mark as dirty.
     */
    void update();

    /**
     * listener should be called while it has changed.
     */
    void setOnSyncListener(BooleanConsumer listener);

    /**
     * listener should be called while it has changed.
     */
    void setOnPersistedListener(BooleanConsumer listener);


    <T> T readRaw();

    /**
     * set persisted prefix name
     */
    @Nullable
    String getPersistedPrefixName();

    /**
     * set persisted prefix name
     */
    void setPersistedPrefixName(String name);

    default String getPersistedKey() {
        var fieldKey = getKey();
        String key = fieldKey.getPersistentKey();
        if (Strings.isNullOrEmpty(key)) {
            key = fieldKey.getName();
        }
        if (!Strings.isNullOrEmpty(getPersistedPrefixName())) {
            key = getPersistedPrefixName() + "." + key;
        }
        return key;
    }

}
