package com.lowdragmc.lowdraglib.syncdata.managed;

class SimpleObjectRef extends ManagedRef {
    private Object oldValue;

    SimpleObjectRef(IManagedVar<?> field) {
        super(field);
        oldValue = getField().value();
    }

    @Override
    public void update() {
        Object newValue = getField().value();
        if ((oldValue == null && newValue != null) || (oldValue != null && newValue == null)) {
            oldValue = getKey().getAccessor().copyForManaged(newValue);
            markAsDirty();
        } else if (oldValue != null && getKey().getAccessor().areDifferent(oldValue, newValue)) {
            oldValue = getKey().getAccessor().copyForManaged(newValue);
            markAsDirty();
        }
    }
}
