package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.managed.IManagedVar;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.ObjectTypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.PrimitiveTypedPayload;


public abstract class SimpleObjectAccessor extends ManagedAccessor {

    private final Class<?>[] operandTypes;

    protected SimpleObjectAccessor(Class<?>... operandTypes) {
        this.operandTypes = operandTypes;
    }

    @Override
    public Class<?>[] operandTypes() {
        return operandTypes;
    }

    public abstract ObjectTypedPayload<?> createEmpty();

    @Override
    public ITypedPayload<?> readManagedField(AccessorOp op, IManagedVar<?> field) {
        var value = field.value();
        if (value != null) {
            //noinspection unchecked
            return ((ObjectTypedPayload)createEmpty()).setPayload(value);
        }
        return PrimitiveTypedPayload.ofNull();
    }

    @Override
    public void writeManagedField(AccessorOp op, IManagedVar<?> field, ITypedPayload<?> payload) {
        if (payload instanceof ObjectTypedPayload<?> object) {
            //noinspection unchecked
            ((IManagedVar<Object>) field).set(object.getPayload());
        }
        if(payload instanceof PrimitiveTypedPayload<?> primitive && primitive.isNull()) {
            field.set(null);
        }
    }

}
