package com.lowdragmc.lowdraglib.syncdata.field;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.accessor.IArrayLikeAccessor;
import com.lowdragmc.lowdraglib.syncdata.managed.*;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public final class ManagedKey {
    @Getter
    private final String name;
    @Getter
    private final boolean isDestSync;
    @Getter
    private final boolean isPersist;
    @Getter
    private final boolean isDrop;
    @Nullable
    @Getter
    private String persistentKey;
    @Getter
    private final boolean isLazy;
    @Getter
    private final Type contentType;
    @Getter
    private final Field rawField;
    @Getter
    private boolean isReadOnlyManaged;
    @Getter
    @Nullable
    private Method onDirtyMethod, serializeMethod, deserializeMethod;

    public void setPersistentKey(@Nullable String persistentKey) {
        this.persistentKey = persistentKey;
    }

    public void setRedOnlyManaged(@Nullable Method onDirtyMethod, Method serializeMethod, Method deserializeMethod) {
        this.isReadOnlyManaged = true;
        this.onDirtyMethod = onDirtyMethod;
        this.serializeMethod = serializeMethod;
        this.deserializeMethod = deserializeMethod;
    }

    public ManagedKey(String name, boolean isDestSync, boolean isPersist, boolean isDrop, boolean isLazy, Type contentType, Field rawField) {
        this.name = name;
        this.isDestSync = isDestSync;
        this.isPersist = isPersist;
        this.isDrop = isDrop;
        this.isLazy = isLazy;
        this.contentType = contentType;
        this.rawField = rawField;
    }

    private IAccessor<?> fieldAccessor;

    public IAccessor<?> getFieldAccessor() {
        if (fieldAccessor == null) {
            fieldAccessor = TypedPayloadRegistries.findByType(contentType);
        }
        return fieldAccessor;
    }

    public IRef createRef(Object instance) {
        return getFieldAccessor().createRef(this, instance);

        try {

            var accessor = getFieldAccessor();

            if(accessor instanceof IArrayLikeAccessor arrayLikeAccessor) {

                if(!accessor.isReadOnly() || !arrayLikeAccessor.getChildAccessor().isReadOnly()) {
                    return new ManagedArrayLikeRef(DirectField.of(rawField, instance)).setKey(this);
                }
//                else if (isReadOnlyManaged()) {
//                    return new ReadOnlyManagedArrayLikeRef(ReadOnlyManagedField.of(rawField, instance, onChangedMethod, serializeMethod, deserializeMethod), isLazy).setKey(this);
//                }
                try {
                    rawField.setAccessible(true);
                    return new ReadonlyArrayRef(rawField.get(instance)).setKey(this);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            if (!accessor.isReadOnly()) {
                return DirectRef.create(DirectField.of(rawField, instance)).setKey(this);
            } else if (isReadOnlyManaged()) {
                return DirectRef.create(ReadOnlyDirectField.of(rawField, instance, onDirtyMethod, serializeMethod, deserializeMethod)).setKey(this);
            }
            try {
                rawField.setAccessible(true);
                return new ReadonlyRef(rawField.get(instance)).setKey(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }catch (Exception e) {
            throw new IllegalStateException("Failed to create ref of " + this.name + " with type:" + this.rawField.getType().getCanonicalName(), e);
        }
    }
}
