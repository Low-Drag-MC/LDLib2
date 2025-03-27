package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.utils.PersistedParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

public interface IPersistedSerializable extends INBTSerializable<CompoundTag> {

    @Override
    default CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return PersistedParser.serializeNBT(this, provider);
    }

    @Override
    default void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        PersistedParser.deserializeNBT(tag, this, provider);
    }

}
