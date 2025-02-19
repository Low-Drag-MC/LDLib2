package com.lowdragmc.lowdraglib.syncdata.payload;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import javax.annotation.Nullable;

/**
 * A special payload that can be used to send serialized data to the client.
 */
public interface ITypedPayload<T> {

    byte getType();

    void writePayload(FriendlyByteBuf buf);

    void readPayload(FriendlyByteBuf buf);

    @Nullable
    Tag serializeNBT();

    void deserializeNBT(Tag tag);

    T getPayload();

    boolean isPrimitive();

    default Object copyForManaged(Object value) {
        return value;
    }
}
