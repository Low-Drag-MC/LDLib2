package com.lowdragmc.lowdraglib2.syncdata;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.utils.INBTSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;

import org.jetbrains.annotations.NotNull;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;

/**
 * Class with this interface can serialize and deserialize itself by detecting fields with
 * {@link Persisted} and {@link Configurable} annotation.
 */
public interface IPersistedSerializable extends INBTSerializable<CompoundTag> {

    default void beforeSerialize() {

    }

    @Override
    default CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return PersistedParser.serializeNBT(this, provider);
    }

    default void writeToBuff(ByteBuf buf) {
        PersistedParser.writeBuff(buf, this);
    }

    default Tag serializeAdditionalNBT(HolderLookup.@NotNull Provider provider) {
        return EndTag.INSTANCE;
    }

    default void afterSerialize() {

    }

    default void beforeDeserialize() {

    }

    @Override
    default void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        PersistedParser.deserializeNBT(tag, this, provider);
    }

    default void readFromBuff(ByteBuf buf) {
        PersistedParser.readBuff(buf, this);
    }

    default void deserializeAdditionalNBT(Tag tag, HolderLookup.@NotNull Provider provider) {

    }

    default void afterDeserialize() {

    }

    default CompoundTag serializeToCompound() {
        return serializeNBT(Platform.getFrozenRegistry());
    }

    default void deserializeFromCompound(@NotNull CompoundTag tag) {
        deserializeNBT(Platform.getFrozenRegistry(), tag);
    }
}
