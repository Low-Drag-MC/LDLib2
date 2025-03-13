package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.syncdata.payload.FriendlyBufPayload;
import com.mojang.serialization.DynamicOps;

import java.util.function.Predicate;

/**
 * Accessor is a class that can read and write a field of a specific type.
 */
public interface IAccessor extends Predicate<Class<?>> {
    /**
     * Read a field by the given dynamic ops type.
     *
     * @param op    The dynamic ops object.
     * @param field The field to read.
     * @param <T>   The type of the dynamic ops object.
     * @return The value of the field in the given dynamic ops type.
     */
    <T> T readField(DynamicOps<T> op, IRef field);

    /**
     * Write the given value (dynamic op type) to the field .
     *
     * @param op      The dynamic ops object.
     * @param field   The field to write.
     * @param payload The value to write.
     * @param <T>     The type of the dynamic ops object.
     */
    <T> void writeField(DynamicOps<T> op, IRef field, T payload);

    void readStreamField(FriendlyBufPayload buffer, IRef field);

    void writeStreamField(FriendlyBufPayload buffer, IRef field);

    /**
     * If the field is a read only field, which means the instance of the field cannot be changed.
     */
    boolean isReadOnly();

    /**
     * Get the types that this accessor can operate on.
     */
    Class<?>[] operandTypes();

    /**
     * If the accessor support child class.
     */
    default boolean supportChildClass() {
        return false;
    }

    /**
     * Test if the given type is supported by this accessor.
     *
     * @param type The type to test.
     * @return True if the type is supported.
     */
    default boolean test(Class<?> type) {
        for (Class<?> aClass : operandTypes()) {
            if (aClass == type) {
                return true;
            } else if (supportChildClass() && aClass.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

}
