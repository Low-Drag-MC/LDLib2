package com.lowdragmc.lowdraglib2.syncdata.field;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ManagedCodec;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import lombok.Getter;
import lombok.ToString;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;

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

    public ManagedKey(String name,
                      boolean isDestSync,
                      boolean isPersist,
                      boolean isDrop,
                      boolean isLazy,
                      Type contentType,
                      Field rawField) {
        this.name = name;
        this.isDestSync = isDestSync;
        this.isPersist = isPersist;
        this.isDrop = isDrop;
        this.isLazy = isLazy;
        this.contentType = contentType;
        this.rawField = rawField;
    }

    public Class<?> getClazzType() {
        return getClazzType(contentType);
    }

    public static Class<?> getClazzType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            // e.g., List<String>
            ParameterizedType paramType = (ParameterizedType) type;
            return (Class<?>) paramType.getRawType();

        } else if (type instanceof GenericArrayType) {
            // e.g., T[]
            GenericArrayType arrayType = (GenericArrayType) type;
            Type componentType = arrayType.getGenericComponentType();
            Class<?> componentClass = getClazzType(componentType);
            // array
            return Array.newInstance(componentClass, 0).getClass();

        } else if (type instanceof TypeVariable<?>) {
            // e.g., T
            TypeVariable<?> typeVar = (TypeVariable<?>) type;
            Type[] bounds = typeVar.getBounds();
            // else Object.class
            return bounds.length > 0 ? getClazzType(bounds[0]) : Object.class;

        } else if (type instanceof WildcardType) {
            // e.g., ? extends Number
            var wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            return upperBounds.length > 0 ? getClazzType(upperBounds[0]) : Object.class;
        }
        return Object.class;
    }


    private IAccessor<?> fieldAccessor;

    /**
     * The accessor registered for the field's type. This ignores {@link ManagedCodec}, which may pick a different
     * accessor per holder class; use {@link #getFieldAccessor(Object)} for the one the field is actually saved and
     * synced with.
     *
     * @throws IllegalArgumentException if no accessor is registered for the type
     */
    public IAccessor<?> getFieldAccessor() {
        if (fieldAccessor == null) {
            fieldAccessor = AccessorRegistries.findByType(contentType);
        }
        return fieldAccessor;
    }

    /**
     * The accessor the field is saved and synced with on {@code holder}: the one its {@link ManagedCodec} names if
     * it has one, the one registered for its type otherwise.
     *
     * @param holder the object the field belongs to, whose class decides which {@link ManagedCodec} overrides apply
     * @throws IllegalArgumentException if the {@link ManagedCodec} can't be resolved, or no accessor is registered
     *                                  for the type
     * @see AccessorRegistries#findByManagedCodec(Field, Object)
     */
    public IAccessor<?> getFieldAccessor(Object holder) {
        return rawField.isAnnotationPresent(ManagedCodec.class) ?
                AccessorRegistries.findByManagedCodec(rawField, holder) :
                getFieldAccessor();
    }

    public IRef<?> createRef(Object instance) {
        return getFieldAccessor(instance).createRef(this, instance);
    }
}
