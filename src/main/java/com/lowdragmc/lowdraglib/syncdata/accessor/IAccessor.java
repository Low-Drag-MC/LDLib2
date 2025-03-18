package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * Accessor is a class that can read and write a field of a specific type.
 */
public interface IAccessor<Ref extends IRef> extends Predicate<Class<?>> {
    /**
     * Read a field by the given dynamic ops type.
     *
     * @param op    The dynamic ops object.
     * @param ref The field to read.
     * @param <T>   The type of the dynamic ops object.
     * @return The value of the field in the given dynamic ops type.
     */
    <T> T readField(DynamicOps<T> op, Ref ref);

    /**
     * Write the given value (dynamic op type) to the field .
     *
     * @param op      The dynamic ops object.
     * @param ref   The field to write.
     * @param payload The value to write.
     * @param <T>     The type of the dynamic ops object.
     */
    <T> void writeField(DynamicOps<T> op, Ref ref, T payload);

    /**
     * Read the field value and write it into the buffer.
     * @param buffer The buffer to write.
     * @param ref The field to read.
     */
    void readFieldToStream(RegistryFriendlyByteBuf buffer, Ref ref);

    /**
     * Write the field value from the buffer.
     * @param buffer The buffer to read.
     * @param ref The field to write.
     */
    void writeFieldFromStream(RegistryFriendlyByteBuf buffer, Ref ref);

    /**
     * Create a reference. Which is called by the {@link ManagedKey#createRef(Object)}
     * @param managedKey The managed information of the field.
     * @param holder field holder.
     */
    Ref createRef(ManagedKey managedKey, @Nonnull Object holder);

    /**
     * If the field is a read only field, which means the instance of the field cannot be changed.
     */
    boolean isReadOnly();

    /**
     * Test if the given type is supported by this accessor.
     *
     * @param type The type to test.
     * @return True if the type is supported.
     */
    boolean test(Class<?> type);

}
