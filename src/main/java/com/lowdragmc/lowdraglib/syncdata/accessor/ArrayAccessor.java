package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.managed.*;
import com.lowdragmc.lowdraglib.syncdata.payload.ArrayPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.PrimitiveTypedPayload;
import net.minecraft.core.HolderLookup;

import java.lang.reflect.Array;

public class ArrayAccessor implements IAccessor, IArrayLikeAccessor {

    @Override
    public byte getDefaultType() {
        return TypedPayloadRegistries.getId(ArrayPayload.class);
    }

    @Override
    public void setDefaultType(byte defaultType) {
        throw new UnsupportedOperationException("Cannot set default type for array accessor");
    }

    private final IAccessor childAccessor;
    private final Class<?> childType;

    public ArrayAccessor(IAccessor childAccessor, Class<?> childType) {
        this.childAccessor = childAccessor;
        this.childType = childType;
    }

    @Override
    public ITypedPayload<?> readField(AccessorOp op, IRef field, HolderLookup.Provider provider) {

        if (field instanceof DirectRef managedRef) {
            var managedField = managedRef.getField();
            if (!managedField.isPrimitive() && managedField.value() == null) {
                return PrimitiveTypedPayload.ofNull();
            }
            return readManagedField(op, managedField, provider);
        }

        var obj = field.readRaw();
        if (obj == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(field));
        }

        return readFromReadonlyField(op, obj, provider);
    }

    @Override
    public void writeField(AccessorOp op, IRef field, ITypedPayload<?> payload, HolderLookup.Provider provider) {
        if (field instanceof DirectRef syncedField) {
            var managedField = syncedField.getField();
            writeManagedField(op, managedField, payload, provider);
        }
        var obj = field.readRaw();
        if (obj == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(field));
        }

        writeToReadonlyField(op, obj, payload, provider);
    }

    @Override
    public boolean hasPredicate() {
        return true;
    }

    @Override
    public boolean test(Class<?> type) {
        return type.isArray();
    }

    @Override
    public boolean isReadOnly() {
        return childAccessor.isReadOnly();
    }

    @Override
    public ITypedPayload<?> readManagedField(AccessorOp op, IDirectVar<?> field, HolderLookup.Provider provider) {
        var value = field.value();
        if (value == null) {
            return PrimitiveTypedPayload.ofNull();
        }

        if (!value.getClass().isArray()) {
            throw new IllegalArgumentException("Value %s is not an array".formatted(value));
        }

        var size = Array.getLength(value);
        var result = new ITypedPayload[size];
        if (childAccessor.isReadOnly()) {
            for (int i = 0; i < size; i++) {
                var obj = Array.get(value, i);
                var payload = childAccessor.readFromReadonlyField(op, obj, provider);
                result[i] = payload;
            }
        } else {
            for (int i = 0; i < size; i++) {
                var holder = ManagedArrayItem.of(value, i);
                var payload = childAccessor.readManagedField(op, holder, provider);
                result[i] = payload;
            }

        }
        return ArrayPayload.of(result);
    }

    @Override
    public void writeManagedField(AccessorOp op, IDirectVar<?> field, ITypedPayload<?> payload, HolderLookup.Provider provider) {
        if (payload instanceof PrimitiveTypedPayload<?> primitive && primitive.isNull()) {
            field.set(null);
            return;
        }
        if (!(payload instanceof ArrayPayload arrayPayload)) {
            throw new IllegalArgumentException("Payload %s is not ArrayPayload".formatted(payload));
        }
        boolean isManaged = !childAccessor.isReadOnly();
        var result = field.value();
        if (result == null || Array.getLength(result) != arrayPayload.getPayload().length) {
            if (!isManaged) {
                throw new IllegalArgumentException("The array of %s should not be changed".formatted(childType));
            }
            result = Array.newInstance(childType, arrayPayload.getPayload().length);
        }
        var payloads = arrayPayload.getPayload();
        if (!isManaged) {
            for (int i = 0; i < payloads.length; i++) {
                var obj = Array.get(result, i);
                var item = payloads[i];
                childAccessor.writeToReadonlyField(op, obj, item, provider);
            }
        } else {
            for (int i = 0; i < payloads.length; i++) {
                var holder = ManagedArrayItem.of(result, i);
                childAccessor.writeManagedField(op, holder, payloads[i], provider);
            }
        }

        if (isManaged) {
            //noinspection unchecked
            ((IDirectVar<Object>) field).set(result);
        }
    }

    @Override
    public ITypedPayload<?> readFromReadonlyField(AccessorOp op, Object obj, HolderLookup.Provider provider) {
        var holder = ManagedHolder.of(obj);
        return readManagedField(op, holder, provider);
    }

    @Override
    public void writeToReadonlyField(AccessorOp op, Object obj, ITypedPayload<?> payload, HolderLookup.Provider provider) {
        var holder = ManagedHolder.of(obj);
        writeManagedField(op, holder, payload, provider);
    }

    @Override
    public IAccessor getChildAccessor() {
        return childAccessor;
    }
}
