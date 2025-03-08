package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.payload.ObjectTypedPayload;

import java.util.function.Supplier;

public class ObjectTypedPayloadAccessor<T> extends SimpleObjectAccessor {
    private final Class<T> type;
    private final boolean inherited;
    private final Supplier<? extends ObjectTypedPayload<T>> payloadSupplier;

    public ObjectTypedPayloadAccessor(Class<T> type, boolean inherited, Supplier<? extends ObjectTypedPayload<T>> payloadSupplier) {
        super(type);
        this.type = type;
        this.inherited = inherited;
        this.payloadSupplier = payloadSupplier;
    }

    private ObjectTypedPayload<T> getPayload() {
        return payloadSupplier.get();
    }

    @Override
    public ObjectTypedPayload<?> createEmpty() {
        return getPayload();
    }

    @Override
    public boolean hasPredicate() {
        return inherited;
    }

    @Override
    public boolean test(Class<?> test) {
        return inherited ? type.isAssignableFrom(test) : super.test(test);
    }

    @Override
    public Object copyForManaged(Object value) {
        return getPayload().copyForManaged(value);
    }
}
