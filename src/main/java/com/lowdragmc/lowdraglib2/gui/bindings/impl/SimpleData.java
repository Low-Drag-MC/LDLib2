package com.lowdragmc.lowdraglib2.gui.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.bindings.IData;
import com.lowdragmc.lowdraglib2.gui.bindings.SyncStrategy;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleData<T> implements IData<T> {
    @Setter
    public SyncStrategy s2cStrategy = SyncStrategy.CHANGED_PERIODIC;
    public final Consumer<T> setter;
    public final Supplier<T> getter;

    public SimpleData(Consumer<T> setter, Supplier<T> getter) {
        this.setter = setter;
        this.getter = getter;
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
    public void tickServer() {

    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void markAsChanged() {

    }

    @Override
    public void clearChanged() {

    }

    @Override
    public void writeS2CSyncData(RegistryFriendlyByteBuf syncData) {

    }

    @Override
    public void readC2SSyncData(RegistryFriendlyByteBuf syncData) {

    }
}
