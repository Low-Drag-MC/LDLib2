package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.syncdata.managed.IManagedVar;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;

import java.util.function.Predicate;

/**
 * Accessor is a class that can read and write a field of a specific type.
 */
public interface IAccessor extends Predicate<Class<?>> {

    ITypedPayload<?> readField(AccessorOp op, IRef field);

    void writeField(AccessorOp op, IRef field, ITypedPayload<?> payload);

    default boolean hasPredicate() {
        return false;
    }

    default boolean test(Class<?> type) {
        return false;
    }

    default Class<?>[] operandTypes() {
        return new Class<?>[0];
    }

    boolean isManaged();

    void setDefaultType(byte payloadType);

    byte getDefaultType();


    default ITypedPayload<?> readFromReadonlyField(AccessorOp op, Object obj) {
        throw new UnsupportedOperationException();
    }

    default void writeToReadonlyField(AccessorOp op, Object obj, ITypedPayload<?> payload) {
        throw new UnsupportedOperationException();
    }

    default ITypedPayload<?> readManagedField(AccessorOp op, IManagedVar<?> field) {
        throw new UnsupportedOperationException();
    }

    default void writeManagedField(AccessorOp op, IManagedVar<?> field, ITypedPayload<?> payload) {
        throw new UnsupportedOperationException();
    }

    default Object copyForManaged(Object value) {
        return value;
    }
}
