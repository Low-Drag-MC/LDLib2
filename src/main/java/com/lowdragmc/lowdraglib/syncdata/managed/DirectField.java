package com.lowdragmc.lowdraglib.syncdata.managed;

import lombok.Getter;

import java.lang.reflect.Field;

@Getter
@SuppressWarnings("unchecked")
public class DirectField<T> implements IDirectVar<T> {
    protected final Field field;
    protected final Class<T> type;
    protected final Object instance;

    public static <T> IDirectVar<T> of(Field field, Object instance) {
        return new DirectField<>(field, instance);
    }

    protected DirectField(Field field, Object instance) {
        field.setAccessible(true);
        this.type = (Class<T>) field.getType();
        this.field = field;
        this.instance = instance;
    }

    @Override
    public T value() {
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(T value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
