package com.lowdragmc.lowdraglib2.syncdata.var;

import java.util.Map;

public class ManagedHolderVar<T> implements IVar<T> {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
        int.class, Integer.class,
        long.class, java.lang.Long.class,
        float.class, java.lang.Float.class,
        double.class, java.lang.Double.class,
        boolean.class, java.lang.Boolean.class,
        byte.class, java.lang.Byte.class,
        char.class, Character.class,
        short.class, java.lang.Short.class
    );

    private T value;
    private final Class<T> type;

    private ManagedHolderVar(T value, Class<T> type) {
        this.type = type;
        set(value);
    }

    public static <T> ManagedHolderVar<T> of(T value) {
        return new ManagedHolderVar<>(value, (Class<T>) value.getClass());
    }

    public static <T> ManagedHolderVar<T> ofNull(Class<T> type) {
        return new ManagedHolderVar<>(null, type);
    }

    public static <T> ManagedHolderVar<T> ofType(Class<T> type) {
        if(type.isPrimitive()) {
            type = (Class<T>) PRIMITIVE_TO_WRAPPER.get(type);
        }
        return new ManagedHolderVar<>(null, type);
    }


    @Override
    public T value() {
        return value;
    }

    @Override
    public void set(T value) {
        if (value != null && !type.isInstance(value)) {
            throw new IllegalArgumentException("Value is not of type " + type);
        }
        this.value = value;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public Class<T> getType() {
        return type;
    }
}
