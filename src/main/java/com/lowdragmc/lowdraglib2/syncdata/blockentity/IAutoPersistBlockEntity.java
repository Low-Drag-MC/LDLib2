package com.lowdragmc.lowdraglib2.syncdata.blockentity;

import com.lowdragmc.lowdraglib2.utils.TagUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

/**
 * Interface for block entities that automatically save and load managed data.
 *
 * @see Persisted
 */
public interface IAutoPersistBlockEntity extends IManagedBlockEntity {

    default void saveManagedPersistentData(CompoundTag tag, boolean forDrop) {
        var persistedFields = getRootStorage().getPersistedFields();
        var managedTag = new CompoundTag();
        for (var persistedField : persistedFields) {
            if (forDrop && !persistedField.getKey().isDrop()) {
                continue;
            }
            var data = persistedField.readPersisted(NbtOps.INSTANCE);
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
        for (var ref : refs) {
            var key = ref.getPersistedKey();
            var data = TagUtils.getTagExtended(managedTag, key);
            if (data != null) {
                ref.writePersisted(NbtOps.INSTANCE, data);
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
