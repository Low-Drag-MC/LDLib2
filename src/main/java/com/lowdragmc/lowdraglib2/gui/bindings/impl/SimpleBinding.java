package com.lowdragmc.lowdraglib2.gui.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.bindings.IBinding;
import com.lowdragmc.lowdraglib2.gui.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.IDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib2.syncdata.var.IVar;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class SimpleBinding<T> implements IBinding<T>, IVar<T> {
    @Setter
    public SyncStrategy c2sStrategy = SyncStrategy.CHANGED_PERIODIC;
    public Class<T> clazz;

    // runtime
    private final IAccessor<?> accessor;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private T cachedValue = null;
    private boolean hasChanged = false;

    public SimpleBinding(Class<T> clazz) {
        this(clazz, null);
    }

    public SimpleBinding(@Nonnull T initialValue) {
        this((Class<T>) initialValue.getClass(), initialValue);
    }

    public SimpleBinding(Class<T> clazz, @Nullable T initialValue) {
        this.clazz = clazz;
        this.cachedValue = initialValue;
        this.accessor = AccessorRegistries.findByClass(clazz);
    }

    /// IData
    @Override
    public T getValue() {
        return cachedValue;
    }

    @Override
    public SyncStrategy getC2SStrategy() {
        return c2sStrategy;
    }

    @Override
    public boolean hasChanged() {
        return hasChanged;
    }

    @Override
    public void markAsChanged() {
        hasChanged = true;
    }

    @Override
    public void clearChanged() {
        hasChanged = false;
    }

    @Override
    public void setValueWithoutNotify(T value) {
        cachedValue = value;
    }

    @Override
    public void notifyChange() {
        markAsChanged();
    }

    @Override
    public ISubscription registerListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void writeC2SSyncData(RegistryFriendlyByteBuf buffer) {
        if (accessor instanceof IDirectAccessor directAccessor) {
            directAccessor.readDirectVarToStream(buffer, this);
        } else if (accessor instanceof IReadOnlyAccessor readOnlyAccessor) {
            readOnlyAccessor.readReadOnlyValueToStream(buffer, value());
        } else {
            throw new IllegalStateException("Cannot write sync data for accessor of type " + accessor.getClass().getName());
        }
    }

    @Override
    public void readS2CSyncData(RegistryFriendlyByteBuf buffer) {
        if (accessor instanceof IDirectAccessor directAccessor) {
            directAccessor.writeDirectVarFromStream(buffer, this);
        } else if (accessor instanceof IReadOnlyAccessor readOnlyAccessor) {
            readOnlyAccessor.writeReadOnlyValueFromStream(buffer, value());
        } else {
            throw new IllegalStateException("Cannot read sync data for accessor of type " + accessor.getClass().getName());
        }
    }

    /// IVar
    @Override
    public T value() {
        return cachedValue;
    }

    @Override
    public void set(T value) {
        cachedValue = value;
        listeners.forEach(l -> l.accept(value));
        clearChanged();
    }

    @Override
    public Class<T> getType() {
        return clazz;
    }
}
