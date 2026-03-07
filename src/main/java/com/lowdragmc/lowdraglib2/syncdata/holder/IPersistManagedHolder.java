package com.lowdragmc.lowdraglib2.syncdata.holder;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.TagUtils;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Interface for block entities that automatically save and load managed data.
 *
 * @see Persisted
 */
public interface IPersistManagedHolder extends IManagedHolder {
    default void saveManagedPersistentData(ValueOutput output, boolean forDrop) {
        var persistedFields = getRootStorage().getPersistedFields();
        var managedTag = new CompoundTag();
        DynamicOps<Tag> ops;
        if (output instanceof TagValueOutput tagValueOutput) {
            ops = tagValueOutput.ops;
        } else {
            ops = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
        }
        for (var persistedField : persistedFields) {
            if (forDrop && !persistedField.getKey().isDrop()) {
                continue;
            }
            var data = persistedField.readPersisted(ops);
            if (data != null) {
                TagUtils.setTagExtended(managedTag, persistedField.getPersistedKey(), data);
            }
        }

        var custom = output.child("custom");
        saveCustomPersistedData(custom, forDrop);
        if (custom.isEmpty()) {
            output.discard("custom");
        }

        if (!managedTag.isEmpty()) {
            var managed = output.child("managed");
            managed.store(managedTag);
        }
    }

    default void loadManagedPersistentData(ValueInput input) {
        DynamicOps<Tag> ops;
        if (input instanceof TagValueInput tagValueInput) {
            ops = tagValueInput.context.ops();
        } else {
            ops = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
        }

        var refs = getRootStorage().getPersistedFields();
        input.read("managed", CompoundTag.CODEC).ifPresent(managed -> {
            for (var ref : refs) {
                var key = ref.getPersistedKey();
                var data = TagUtils.getTagExtended(managed, key);
                if (data != null) {
                    ref.writePersisted(ops, data);
                }
            }
        });
        input.child("custom").ifPresent(this::loadCustomPersistedData);
    }


    /**
     * write custom data to the save
     */
    default void saveCustomPersistedData(ValueOutput output, boolean forDrop) {

    }

    /**
     * read custom data from the save
     */
    default void loadCustomPersistedData(ValueInput input) {
    }

}
