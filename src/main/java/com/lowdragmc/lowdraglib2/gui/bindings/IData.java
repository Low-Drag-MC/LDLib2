package com.lowdragmc.lowdraglib2.gui.bindings;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IData<T> {

    T getDataValue();

    void setDataValue(T value);

    /**
     * Get the strategy for synchronizing data from server to client.
     */
    SyncStrategy getS2CStrategy();

    void tickServer();

    boolean hasChanged();

    void markAsChanged();

    void clearChanged();

    default boolean needS2CSync() {
        var strategy = getS2CStrategy();
        return strategy == SyncStrategy.ALWAYS || hasChanged() && strategy != SyncStrategy.NONE;
    }

    void writeS2CSyncData(RegistryFriendlyByteBuf syncData);

    void readC2SSyncData(RegistryFriendlyByteBuf syncData);
}
