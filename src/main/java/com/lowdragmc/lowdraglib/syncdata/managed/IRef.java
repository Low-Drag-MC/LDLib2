package com.lowdragmc.lowdraglib.syncdata.managed;

import com.google.common.base.Strings;
import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;

/**
 * Ref is a reference to a field instance, it's used to detect / manage the field's dirty status.
 * <br>
 * Also, can be used to obtain the internal value.
 */
public interface IRef {
    /**
     * ManagedKey refer to ref's meta info. It's used to get the field's name, type, etc.
     */
    ManagedKey getKey();

    /**
     * The creator of this ref. please refer to {@link IAccessor#createRef(ManagedKey, Object)}
     * <br>
     * This accessor is not same as the accessor in {@link ManagedKey#getFieldAccessor()}, it's the accessor that created this ref.
     */
    IAccessor<?> getAccessor();

    /**
     * Read the real value of the field.
     */
    <T> T readRaw();

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
     * Implement this method to check it has changes (e.g. internal changed, instance change). If it has changed, it should mark as dirty.
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

    @SuppressWarnings("unchecked")
    default <T> T readPersisted(DynamicOps<T> op) {
        return ((IAccessor<IRef>)getAccessor()).readField(op, this);
    }

    @SuppressWarnings("unchecked")
    default <T> void writePersisted(DynamicOps<T> op, T payload) {
        ((IAccessor<IRef>)getAccessor()).writeField(op, this, payload);
    }

    @SuppressWarnings("unchecked")
    default void readSyncToStream(RegistryFriendlyByteBuf buffer) {
        ((IAccessor<IRef>)getAccessor()).readFieldToStream(buffer, this);
    }

    @SuppressWarnings("unchecked")
    default void writeSyncFromStream(RegistryFriendlyByteBuf buffer) {
        ((IAccessor<IRef>)getAccessor()).writeFieldFromStream(buffer, this);
    }
}
