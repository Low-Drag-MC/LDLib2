package com.lowdragmc.lowdraglib.syncdata.payload;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A special payload that can be used to send serialized data to the client.
 */
public interface ITypedPayload<T> {

    byte getType();

    void writePayload(RegistryFriendlyByteBuf buf);

    void readPayload(RegistryFriendlyByteBuf buf);

    @Nullable
    Tag serializeNBT(HolderLookup.Provider provider);

    void deserializeNBT(Tag tag, HolderLookup.Provider provider);

    T getPayload();

    boolean isPrimitive();

    default Object copyForManaged(Object value) {
        return value;
    }

    default boolean areDifferent(@Nonnull Object a, @Nonnull Object b) {
        return !a.equals(b);
    }
}
