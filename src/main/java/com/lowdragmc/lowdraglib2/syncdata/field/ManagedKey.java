package com.lowdragmc.lowdraglib2.syncdata.field;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

@ToString
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
    @ToString.Exclude
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
            fieldAccessor = AccessorRegistries.findByType(contentType);
        }
        return fieldAccessor;
    }

    public IRef<?> createRef(Object instance) {
        return getFieldAccessor().createRef(this, instance);
    }
}
