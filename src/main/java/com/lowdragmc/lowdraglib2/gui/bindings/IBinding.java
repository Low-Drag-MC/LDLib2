package com.lowdragmc.lowdraglib2.gui.bindings;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IBinding<T> extends IDataSource<T>, IObserver<T> {
    /**
     * Get the strategy for synchronizing data from client to server.
     */
    SyncStrategy getC2SStrategy();

    boolean hasChanged();

    void markAsChanged();

    void clearChanged();

    void writeC2SSyncData(RegistryFriendlyByteBuf buffer);

    void readS2CSyncData(RegistryFriendlyByteBuf buffer);

    default boolean needC2SSync() {
        var strategy = getC2SStrategy();
        return strategy == SyncStrategy.ALWAYS || hasChanged() && strategy != SyncStrategy.NONE;
    }
}

