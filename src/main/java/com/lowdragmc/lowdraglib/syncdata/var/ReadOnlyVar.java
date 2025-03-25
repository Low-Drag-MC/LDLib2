package com.lowdragmc.lowdraglib.syncdata.var;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import lombok.Getter;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * ReadOnlyDirectField represents a reference to a read-only value, the value instance is not changeable by default.
 * If the field is marked with {@link com.lowdragmc.lowdraglib.syncdata.annotation.ReadOnlyManaged}, the value instance can be changed.
 */
public final class ReadOnlyVar<T> implements IVar<T> {
    @Getter
    private final IVar<T> var;
    @Getter
    @Nullable
    private final IReadOnlyManagedVar<T> managedVar;
    @Nullable
    private final WeakReference<T> valueCache;

    public ReadOnlyVar(IVar<T> var, @Nullable IReadOnlyManagedVar<T> managedVar) {
        this.var = var;
        this.managedVar = managedVar;
        if (isReadOnlyManaged()) {
            valueCache = null;
        } else {
            valueCache = new WeakReference<>(value());
        }
    }

    public static <T> ReadOnlyVar<T> of(IVar<T> var) {
        return new ReadOnlyVar<>(var, null);
    }

    public static <T> ReadOnlyVar<T> of(IVar<T> var, IReadOnlyManagedVar<T> managedVar) {
        return new ReadOnlyVar<>(var, managedVar);
    }

    public boolean isReadOnlyManaged() {
        return managedVar != null;
    }

    public static <T> ReadOnlyVar<T> of(ManagedKey key, Object instance) {
        return new ReadOnlyVar<>(FieldVar.of(key.getRawField(), instance),
                key.isReadOnlyManaged() ? IReadOnlyManagedVar.fromManagedKey(key, instance) : null);
    }

    @Override
    public T value() {
        return (isReadOnlyManaged() || valueCache == null) ? var.value() : valueCache.get();
    }

    @Override
    public void set(T value) {
        if (isReadOnlyManaged()) {
            var.set(value);
        } else {
            throw new UnsupportedOperationException("Cannot set value to a read-only field");
        }
    }

    @Override
    public Class<T> getType() {
        return var.getType();
    }
}
