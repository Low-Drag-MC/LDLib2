package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ReadOnlyDirectField represents a reference to a read-only value, the value instance is not changeable by default.
 * If the field is marked with {@link com.lowdragmc.lowdraglib.syncdata.annotation.ReadOnlyManaged}, the value instance can be changed.
 */
public final class ReadOnlyDirectField<T> extends DirectField<T> {
    @Getter
    private final boolean isReadOnlyManaged;
    @Nullable
    private final Method onDirtyMethod, serializeMethod, deserializeMethod;
    @Nullable
    private final WeakReference<T> valueCache;

    public ReadOnlyDirectField(Field field, Object instance, boolean isReadOnlyManaged, @Nullable Method onDirtyMethod, @Nullable Method serializeMethod, @Nullable Method deserializeMethod) {
        super(field, instance);
        this.isReadOnlyManaged = isReadOnlyManaged;
        this.onDirtyMethod = onDirtyMethod;
        this.serializeMethod = serializeMethod;
        this.deserializeMethod = deserializeMethod;
        if (isReadOnlyManaged) {
            valueCache = null;
        } else {
            valueCache = new WeakReference<>(value());
        }
    }

    public static <T> ReadOnlyDirectField<T> of(ManagedKey key, Object instance) {
        return new ReadOnlyDirectField<>(key.getRawField(), instance, key.isReadOnlyManaged(), key.getOnDirtyMethod(), key.getDeserializeMethod(), key.getSerializeMethod());
    }

    @Override
    public T value() {
        return (isReadOnlyManaged || valueCache == null) ? super.value() : valueCache.get();
    }

    @Override
    public void set(T value) {
        if (isReadOnlyManaged) {
            super.set(value);
        } else {
            throw new UnsupportedOperationException("Cannot set value to a read-only field");
        }
    }

    public boolean hasDirtyMethod() {
        return onDirtyMethod != null;
    }

    public boolean isDirty(Object obj) {
        if (onDirtyMethod == null) {
            return false;
        }
        try {
            return (boolean) onDirtyMethod.invoke(instance, obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public CompoundTag serializeUid(Object obj) {
        if (serializeMethod == null) {
            throw new UnsupportedOperationException("Cannot serialize uid for a read-only field");
        }
        try {
            return (CompoundTag)serializeMethod.invoke(instance, obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Object deserializeUid(CompoundTag uid) {
        if (deserializeMethod == null) {
            throw new UnsupportedOperationException("Cannot serialize uid for a read-only field");
        }
        try {
            return deserializeMethod.invoke(instance, uid);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
