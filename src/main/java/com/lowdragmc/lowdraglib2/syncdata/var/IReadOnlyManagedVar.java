package com.lowdragmc.lowdraglib2.syncdata.var;

import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface IReadOnlyManagedVar<TYPE> {
    record MethodInstance<TYPE>(Object instance, @Nullable Method onDirtyMethod, Method serializeMethod, Method deserializeMethod) implements IReadOnlyManagedVar<TYPE> {
        @Override
        public Tag serializeUid(TYPE obj) {
            if (serializeMethod == null) {
                throw new UnsupportedOperationException("Cannot serialize uid for a read-only field");
            }
            try {
                return (Tag)serializeMethod.invoke(instance, obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public TYPE deserializeUid(Tag uid) {
            if (deserializeMethod == null) {
                throw new UnsupportedOperationException("Cannot serialize uid for a read-only field");
            }
            try {
                return (TYPE) deserializeMethod.invoke(instance, uid);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean hasDirtyChecker() {
            return onDirtyMethod != null;
        }

        @Override
        public boolean checkIsDirty() {
            try {
                return onDirtyMethod != null && (boolean) onDirtyMethod.invoke(instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static <TYPE> IReadOnlyManagedVar<TYPE> fromManagedKey(ManagedKey key, Object instance) {
        return new MethodInstance<>(instance, key.getOnDirtyMethod(), key.getSerializeMethod(), key.getDeserializeMethod());
    }

    /**
     * Determines if a "dirty checker" method is present for the current object.
     * A "dirty checker" is used to identify if the managed state or value has changed and requires further processing.
     *
     * @return true if a "dirty checker" method is available; false otherwise.
     */
    boolean hasDirtyChecker();

    /**
     * Evaluates whether the associated object or state is "dirty" (i.e., has been modified or requires further action).
     * This method internally checks if a custom "dirty checker" is available
     */
    boolean checkIsDirty();

    /**
     * Serializes a unique identifier (UID) for the specified object into a CompoundTag.
     * This method utilizes a provided serialization mechanism to convert the object into
     * a CompoundTag representation, which is a structured format for storing data.
     *
     * @param obj the object for which a unique identifier is to be serialized
     * @return a CompoundTag representing the serialized unique identifier of the object
     */
    Tag serializeUid(TYPE obj);

    /**
     * Deserializes a unique identifier (UID) from a given CompoundTag into an object of the target type.
     * This method invokes a deserialization mechanism to reconstruct the object from its serialized UID representation.
     *
     * @param uid the CompoundTag representing the serialized unique identifier of the object
     * @return the deserialized object of the target type
     */
    TYPE deserializeUid(Tag uid);

}
