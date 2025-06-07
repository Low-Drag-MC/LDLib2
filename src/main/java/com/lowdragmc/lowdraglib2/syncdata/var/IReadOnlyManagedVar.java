package com.lowdragmc.lowdraglib2.syncdata.var;

import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface IReadOnlyManagedVar<TYPE> {
    record MethodInstance<TYPE>(Object instance, @Nullable Method onDirtyMethod, Method serializeMethod, Method deserializeMethod) implements IReadOnlyManagedVar<TYPE> {
        public CompoundTag serializeUid(TYPE obj) {
            if (serializeMethod == null) {
                throw new UnsupportedOperationException("Cannot serialize uid for a read-only field");
            }
            try {
                return (CompoundTag)serializeMethod.invoke(instance, obj);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public TYPE deserializeUid(CompoundTag uid) {
            if (deserializeMethod == null) {
                throw new UnsupportedOperationException("Cannot serialize uid for a read-only field");
            }
            try {
                return (TYPE) deserializeMethod.invoke(instance, uid);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static <TYPE> IReadOnlyManagedVar<TYPE> fromManagedKey(ManagedKey key, Object instance) {
        return new MethodInstance<>(instance, key.getOnDirtyMethod(), key.getSerializeMethod(), key.getDeserializeMethod());
    }

    CompoundTag serializeUid(TYPE obj);

    TYPE deserializeUid(CompoundTag uid);

}
