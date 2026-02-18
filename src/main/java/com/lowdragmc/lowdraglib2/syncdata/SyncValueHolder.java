package com.lowdragmc.lowdraglib2.syncdata;

import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

@ToString
public class SyncValueHolder<T> {
    @Getter(lazy = true)
    private final static Field valueField = createCacheValueField();

    public final Type type;
    public final ManagedKey managedKey;
    @ToString.Exclude
    public final IRef<?> ref;
    @Nullable
    @Setter
    @ToString.Exclude
    private T value;

    public SyncValueHolder(String name, Type type, @Nullable T value) {
        this.type = type;
        this.value = value;
        this.managedKey = new ManagedKey(name, true, false, false, false, type, getValueField());
        this.ref = managedKey.createRef(this);
    }

    private static Field createCacheValueField() {
        try {
            Field field = SyncValueHolder.class.getDeclaredField("value");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public T getValue() {
        return value;
    }
}
