package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.managed.DirectRef;
import com.lowdragmc.lowdraglib.syncdata.managed.IDirectVar;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public interface IDirectAccessor<VAR extends IDirectVar<?>> extends IAccessor<DirectRef<VAR>> {
    /**
     * Read the payload from the var value to the given dynamic ops. The field's internal value is guaranteed to be non-empty.
     * @param op The dynamic ops.
     * @param var The var to read from.
     * @return The payload.
     */
    <T> T readDirectVar(DynamicOps<T> op, VAR var);

    /**
     * Write the payload to the var. The payload is guaranteed to be non-empty.
     * @param op The dynamic ops.
     * @param var The var to write to.
     * @param payload The payload to write.
     */
    <T> void writeDirectVar(DynamicOps<T> op, VAR var, T payload);

    /**
     * Read the var value and write it into the buffer. The field's internal value is guaranteed to be non-empty.
     * @param buffer The buffer to write.
     * @param var The var to read.
     */
    void readDirectVarToStream(RegistryFriendlyByteBuf buffer, VAR var);

    /**
     * Write the var value from the buffer. The field's internal value is guaranteed to be non-empty.
     * @param buffer The buffer to read.
     * @param var The var to write.
     */
    void writeDirectVarFromStream(RegistryFriendlyByteBuf buffer, VAR var);

    /**
     * Create a direct reference with the given var
     * @param managedKey The managed information of the field.
     * @param var The field to create a reference.
     */
    DirectRef<VAR> createDirectRef(ManagedKey managedKey, VAR var);

    /**
     * Create a direct var for the field. which is called by the {@link #createRef(ManagedKey, Object)}
     * @param managedKey The managed information of the field.
     * @param holder The holder of the field.
     */
    VAR createDirectVar(ManagedKey managedKey, @NotNull Object holder);

    @Override
    default DirectRef<VAR> createRef(ManagedKey managedKey, @NotNull Object holder) {
        return createDirectRef(managedKey, createDirectVar(managedKey, holder));
    }

    @Override
    default boolean isReadOnly() {
        return false;
    }

    @Override
    default <T> T readField(DynamicOps<T> op, DirectRef<VAR> ref) {
        var managedField = ref.getField();
        if (!managedField.isPrimitive() && managedField.value() == null) {
            return op.empty();
        }
        return readDirectVar(op, managedField);
    }

    @Override
    default <T> void writeField(DynamicOps<T> op, DirectRef<VAR> ref, T payload) {
        var managedField = ref.getField();
        if (!managedField.isPrimitive() && payload == op.empty()) {
            managedField.set(null);
            return;
        }
        writeDirectVar(op, managedField, payload);
    }

    @Override
    default void writeFieldFromStream(RegistryFriendlyByteBuf buffer, DirectRef<VAR> ref) {
        var managedField = ref.getField();
        if (!managedField.isPrimitive() && buffer.readBoolean()) {
            managedField.set(null);
            return;
        }
        writeDirectVarFromStream(buffer, managedField);
    }

    @Override
    default void readFieldToStream(RegistryFriendlyByteBuf buffer, DirectRef<VAR> ref) {
        var managedField = ref.getField();
        if (!managedField.isPrimitive() && managedField.value() == null) {
            buffer.writeBoolean(true);
            return;
        }
        buffer.writeBoolean(false);
        readDirectVarToStream(buffer, managedField);
    }
}
