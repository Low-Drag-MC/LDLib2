package com.lowdragmc.lowdraglib.syncdata.managed;

import java.util.Map;

public class ManagedHolder<T> implements IDirectVar<T> {
    private static Map<Class<?>, Class<?>> primitiveToWrapper = Map.of(
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

    public ManagedHolder(T value, Class<T> type) {
        this.type = type;
        set(value);
    }

    public static <T> ManagedHolder<T> of(T value) {
        return new ManagedHolder<>(value, (Class<T>) value.getClass());
    }

    public static <T> ManagedHolder<T> ofType(Class<T> type) {
        if(type.isPrimitive()) {
            type = (Class<T>) primitiveToWrapper.get(type);
        }
        return new ManagedHolder<>(null, type);
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
