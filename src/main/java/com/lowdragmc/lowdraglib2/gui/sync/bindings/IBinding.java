package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IBinding<T> extends IDataSource<T>, IObserver<T> {
    /**
     * Get the strategy for synchronizing data from client to server.
     */
    SyncStrategy getC2SStrategy();

    /**
     * Determines whether the binding accepts synchronization data from server to client (S2C).
     *
     * @return true if this binding allows S2C synchronization data, false otherwise.
     */
    boolean acceptS2C();

    /**
     * Retrieves the unique name of this binding or data source.
     *
     * @return the name of the binding or data source as a String.
     */
    String name();

    /**
     * Updates the client-side logic for the binding.
     * This method is intended to be called on each client tick to perform any necessary updates
     * or processing associated with the binding.
     */
    void tickClient();

    /**
     * Checks whether the binding or data source has changed since the last synchronization.
     *
     * @return true if the associated data or state has been modified, false otherwise.
     */
    boolean hasChanged();

    /**
     * Marks the associated data or state as having been changed manually.
     * This method is typically used to indicate that the binding's value or state
     * requires synchronization or further processing during the next update cycle.
     */
    void markAsChanged();

    /**
     * Resets the changed state for the binding or data source.
     * This method is typically called after changes have been processed
     * or synchronized, ensuring that the state reflects no pending updates.
     */
    void clearChanged();

    /**
     * Writes client-to-server (C2S) synchronization data for the current binding into the provided buffer.
     * This method is responsible for serializing changes or updates in the binding's state
     * that need to be communicated to the server.
     *
     * @param buffer the buffer into which the C2S synchronization data should be written
     */
    void writeC2SSyncData(RegistryFriendlyByteBuf buffer);

    /**
     * Reads server-to-client (S2C) synchronization data from the provided buffer.
     * This method is responsible for deserializing and processing data sent from the server,
     * allowing the client to update its state and remain in sync with the server.
     * You should always check {@link #acceptS2C()} before calling this method to avoid unexpected updates, e.g., attacks
     *
     * @param buffer the buffer containing the S2C synchronization data to be read
     */
    void readS2CSyncData(RegistryFriendlyByteBuf buffer);

    /**
     * Determines whether the current binding requires synchronization of its data
     * from client to server (C2S). The necessity for synchronization is based on the
     * combination of the synchronization strategy defined by the {@code getC2SStrategy()} method
     * and the change state of the binding indicated by {@code hasChanged()}.
     *
     * @return {@code true} if client-to-server synchronization is needed, {@code false} otherwise.
     */
    default boolean needC2SSync() {
        var strategy = getC2SStrategy();
        return strategy == SyncStrategy.ALWAYS || hasChanged() && strategy != SyncStrategy.NONE;
    }
}

