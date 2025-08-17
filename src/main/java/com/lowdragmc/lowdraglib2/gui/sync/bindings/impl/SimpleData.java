package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.SyncValueHolder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IData;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleData<T> implements IData<T> {
    public final Consumer<T> setter;
    public final Supplier<T> getter;
    public final SyncValueHolder syncValueHolder;

    // runtime
    @Setter
    private SyncStrategy s2cStrategy = SyncStrategy.CHANGED_PERIODIC;
    @Setter
    private boolean acceptC2S;

    public SimpleData(String name, Type type, Consumer<T> setter, Supplier<T> getter) {
        this.setter = setter;
        this.getter = getter;
        this.syncValueHolder = new SyncValueHolder(name, type, getter.get());
    }

    @Override
    public String name() {
        return syncValueHolder.managedKey.getName();
    }

    @Override
    public T getDataValue() {
        return getter.get();
    }

    @Override
    public void setDataValue(T value) {
        setter.accept(value);
    }

    @Override
    public SyncStrategy getS2CStrategy() {
        return s2cStrategy;
    }

    @Override
    public boolean acceptC2S() {
        return acceptC2S;
    }

    @Override
    public void tickServer() {
        syncValueHolder.setValue(getDataValue());
        if (s2cStrategy == SyncStrategy.CHANGED_PERIODIC) {
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
    public void writeS2CSyncData(RegistryFriendlyByteBuf buffer) {
        syncValueHolder.ref.readSyncToStream(buffer);
    }

    @Override
    public void readC2SSyncData(RegistryFriendlyByteBuf buffer) {
        syncValueHolder.ref.writeSyncFromStream(buffer);
        setDataValue(syncValueHolder.getValue());
    }

    @Override
    public String toString() {
        return "%s[%s, %s]".formatted(name(), getS2CStrategy(), acceptC2S());
    }
}
