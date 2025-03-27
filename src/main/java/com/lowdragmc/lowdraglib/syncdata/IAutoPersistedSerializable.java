package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.registry.ILDLRegister;
import com.lowdragmc.lowdraglib.registry.ILDLRegisterClient;
import com.lowdragmc.lowdraglib.utils.PersistedParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/6/3
 * @implNote IAutoPersistedSerializable
 */
@ParametersAreNonnullByDefault
public interface IAutoPersistedSerializable extends INBTSerializable<CompoundTag> {
    @Override
    default CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        PersistedParser.serializeNBT(tag, this.getClass(), this, provider);
        if (this instanceof ILDLRegisterClient registerClient) {
            tag.putString("_type", registerClient.name());
        } else if (this instanceof ILDLRegister register) {
            tag.putString("_type", register.name());
        }
        return tag;
    }

    @Override
    default void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        PersistedParser.deserializeNBT(tag, this, provider);
    }

}
