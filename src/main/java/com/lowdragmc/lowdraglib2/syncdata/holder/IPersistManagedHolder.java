package com.lowdragmc.lowdraglib2.syncdata.holder;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.TagUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/**
 * Interface for block entities that automatically save and load managed data.
 *
 * @see Persisted
 */
public interface IPersistManagedHolder extends IManagedHolder {
    default void saveManagedPersistentData(CompoundTag tag, boolean forDrop) {
        var persistedFields = getRootStorage().getPersistedFields();
        var managedTag = new CompoundTag();
        var ctx = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
        for (var persistedField : persistedFields) {
            if (forDrop && !persistedField.getKey().isDrop()) {
                continue;
            }
            var data = persistedField.readPersisted(ctx);
            if (data != null) {
                TagUtils.setTagExtended(managedTag, persistedField.getPersistedKey(), data);
            }
        }

        var customTag = new CompoundTag();
        saveCustomPersistedData(customTag, forDrop);

        if (!managedTag.isEmpty()) {
            tag.put("managed", managedTag);
        }
        if (!customTag.isEmpty()) {
            managedTag.put("custom", customTag);
        }
    }

    default void loadManagedPersistentData(CompoundTag tag) {
        var refs = getRootStorage().getPersistedFields();
        var managedTag = tag.getCompound("managed");
        var ctx = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
        for (var ref : refs) {
            var key = ref.getPersistedKey();
            var data = TagUtils.getTagExtended(managedTag, key);
            if (data != null) {
                ref.writePersisted(ctx, data);
            }
        }
        loadCustomPersistedData(tag.getCompound("custom"));
    }


    /**
     * write custom data to the save
     */
    default void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {

    }

    /**
     * read custom data from the save
     */
    default void loadCustomPersistedData(CompoundTag tag) {
    }

}
