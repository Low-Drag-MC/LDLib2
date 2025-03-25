package com.lowdragmc.lowdraglib.syncdata.accessor.readonly;

import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib.syncdata.var.ReadOnlyVar;
import com.lowdragmc.lowdraglib.syncdata.ref.ReadOnlyRef;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public interface IReadOnlyAccessor<TYPE> extends IAccessor<TYPE> {
    /**
     * Read the payload from the internal value to the given dynamic ops.
     * @param op The dynamic ops.
     * @param value The internal value.
     * @return The payload.
     */
    <T> T readReadOnlyValue(DynamicOps<T> op, @Nonnull TYPE value);

    /**
     * Write the payload to the internal value.
     * @param op The dynamic ops.
     * @param value The internal value.
     * @param payload The payload to write.
     */
    <T> void writeReadOnlyValue(DynamicOps<T> op, TYPE value, T payload);

    /**
     * Read the internal value and write it into the buffer.
     * @param buffer The buffer to write.
     * @param value The internal value to read.
     */
    void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @Nonnull TYPE value);

    /**
     * Write the internal value from the buffer.
     * @param buffer The buffer to read.
     * @param value The internal value to write.
     */
    void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @Nonnull TYPE value);

    /**
     * Create a readonly reference with the given value.
     * @param managedKey The managed information of the field.
     * @param field The field value accessor.
     * @return
     */
    default ReadOnlyRef<TYPE> createReadOnlyRef(ManagedKey managedKey, ReadOnlyVar<TYPE> field) {
        return new ReadOnlyRef<>(field, managedKey, this);
    }

    @Override
    default ReadOnlyRef<TYPE> createRef(ManagedKey managedKey, @NotNull Object holder) {
        return createReadOnlyRef(managedKey, ReadOnlyVar.of(managedKey, holder));
    }

    @Override
    default boolean isReadOnly() {
        return true;
    }

    @Override
    default <T> T readField(DynamicOps<T> op, IRef<TYPE> ref) {
        var value = ref.readRaw();
        if (value == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(ref.getKey()));
        }
        return readReadOnlyValue(op, value);
    }

    @Override
    default <T> void writeField(DynamicOps<T> op, IRef<TYPE> ref, T payload) {
        var value = ref.readRaw();
        if (value == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(ref.getKey()));
        }
        writeReadOnlyValue(op, value, payload);
    }

    @Override
    default void readFieldToStream(RegistryFriendlyByteBuf buffer, IRef<TYPE> ref) {
        var value = ref.readRaw();
        if (value == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(ref.getKey()));
        }
        readReadOnlyValueToStream(buffer, value);
    }

    @Override
    default void writeFieldFromStream(RegistryFriendlyByteBuf buffer, IRef<TYPE> ref) {
        var value = ref.readRaw();
        if (value == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(ref.getKey()));
        }
        writeReadOnlyValueFromStream(buffer, value);
    }

}
