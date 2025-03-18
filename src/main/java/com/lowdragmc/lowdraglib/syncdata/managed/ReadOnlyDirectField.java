package com.lowdragmc.lowdraglib.syncdata.managed;

import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author KilaBash
 * @date 2023/2/19
 * @implNote ReadOnlyManagedField
 */
public class ReadOnlyDirectField extends DirectField {

    protected final Method onDirtyMethod, serializeMethod, deserializeMethod;

    protected ReadOnlyDirectField(Field field, Object instance, Method onDirtyMethod, Method serializeMethod, Method deserializeMethod) {
        super(field, instance);
        this.onDirtyMethod = onDirtyMethod;
        this.serializeMethod = serializeMethod;
        this.deserializeMethod = deserializeMethod;
    }

    public static ReadOnlyDirectField of(Field field, Object instance, Method onDirtyMethod, Method serializeMethod, Method deserializeMethod) {
        return new ReadOnlyDirectField(field, instance, onDirtyMethod, serializeMethod, deserializeMethod);
    }

    public boolean isDirty(Object obj) {
        try {
            return (boolean) onDirtyMethod.invoke(instance, obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public CompoundTag serializeUid(Object obj) {
        try {
            return (CompoundTag)serializeMethod.invoke(instance, obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Object deserializeUid(CompoundTag uid) {
        try {
            return deserializeMethod.invoke(instance, uid);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
