package com.lowdragmc.lowdraglib.syncdata.ref;

import com.lowdragmc.lowdraglib.syncdata.accessor.arraylike.IArrayLikeAccessor;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;
import java.lang.reflect.Array;

/**
 * ArrayRef is used to manage an array of references.
 * @param <TYPE> the type of the array element
 * @param <TYPE_ARRAY> the type of the array, which is an array of TYPE. e.g. int[] for TYPE=int, or String[] for TYPE=String
 */
public interface IArrayRef<TYPE, TYPE_ARRAY> extends IRef<TYPE_ARRAY> {

    /**
     * Get the references of the array. The returned array should be the same length as the array.
     * it will return null if the array is null.
     */
    @Nullable
    IRef<TYPE>[] getRefs();

    /**
     * Update the references of the array. it will affect the returned value of {@link #getRefs()}.
     * @param values the new array values. it will be null if the array is null.
     */
    void updateRefs(@Nullable TYPE_ARRAY values);

    @Override
    IArrayLikeAccessor<TYPE, TYPE_ARRAY> getAccessor();

    /**
     * Get the length of the array.
     */
    default int length(TYPE_ARRAY array) {
        return Array.getLength(array);
    }

    @Override
    default void readSyncToStream(RegistryFriendlyByteBuf buffer) {
        var refs = getRefs();
        if (refs == null) {
            buffer.writeBoolean(true);
            IRef.super.readSyncToStream(buffer);
            return;
        }
        buffer.writeBoolean(false);
        buffer.writeVarInt(refs.length);
        for (IRef<TYPE> typeiRef : refs) {
            typeiRef.readSyncToStream(buffer);
        }
    }

    @Override
    default void writeSyncFromStream(RegistryFriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            IRef.super.writeSyncFromStream(buffer);
            return;
        }
        var refs = getRefs();
        var length = buffer.readVarInt();
        if (refs == null || length != refs.length) {
            updateRefs((TYPE_ARRAY) Array.newInstance(getAccessor().getChildType(), length));
            refs = getRefs();
        }
        for (IRef<TYPE> typeiRef : refs) {
            typeiRef.writeSyncFromStream(buffer);
        }
    }
}
