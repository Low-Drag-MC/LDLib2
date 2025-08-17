package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.SyncValueHolder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBinding;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleBinding<T> implements IBinding<T> {

    public final SyncValueHolder syncValueHolder;

    // runtime
    @Setter
    private SyncStrategy c2sStrategy = SyncStrategy.CHANGED_PERIODIC;
    @Setter
    private boolean acceptS2C = false;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    public SimpleBinding(String name, Type type, @Nullable T initialValue) {
        syncValueHolder = new SyncValueHolder(name, type, initialValue);
    }

    @Override
    public String name() {
        return syncValueHolder.managedKey.getName();
    }

    /// IData
    @Override
    public T getValue() {
        return syncValueHolder.getValue();
    }

    @Override
    public SyncStrategy getC2SStrategy() {
        return c2sStrategy;
    }

    @Override
    public boolean acceptS2C() {
        return acceptS2C;
    }

    @Override
    public void tickClient() {
        if (c2sStrategy == SyncStrategy.CHANGED_PERIODIC) {
            syncValueHolder.ref.update();
        }
    }

    @Override
    public boolean hasChanged() {
        return syncValueHolder.ref.isSyncDirty();
    }

    @Override
    public void markAsChanged() {
        syncValueHolder.ref.markAsDirty();
    }

    @Override
    public void clearChanged() {
        syncValueHolder.ref.clearSyncDirty();
    }

    @Override
    public void setValueWithoutNotify(T value) {
        syncValueHolder.setValue(value);
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
        syncValueHolder.ref.readSyncToStream(buffer);
    }

    @Override
    public void readS2CSyncData(RegistryFriendlyByteBuf buffer) {
        syncValueHolder.ref.writeSyncFromStream(buffer);
        listeners.forEach(l -> l.accept(getValue()));
    }

    @Override
    public String toString() {
        return "%s[%s, %s]".formatted(name(), getC2SStrategy(), acceptS2C());
    }
}
