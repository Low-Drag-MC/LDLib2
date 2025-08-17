package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IData<T> {
    /**
     * Get the strategy for synchronizing data from server to client.
     */
    SyncStrategy getS2CStrategy();

    /**
     * Determines whether synchronization data from client to server (C2S) is accepted.
     *
     * @return true if the data source or binding accepts C2S synchronization; false otherwise.
     */
    boolean acceptC2S();

    /**
     * Retrieves the name of this data source or binding.
     *
     * @return the name as a String.
     */
    String name();

    /**
     * Retrieves the current real value of the data associated with this element.
     *
     * @return the current data value of type T
     */
    T getDataValue();

    /**
     * Sets the current real value of this data source or binding.
     * This method updates the internal state with the provided value.
     *
     * @param value the new data value to set, of type T
     */
    void setDataValue(T value);

    /**
     * Updates the server-side logic for the data or binding.
     * This method is invoked periodically, typically on each server tick, to perform tasks
     * such as processing changes, maintaining state, or syncing data.
     *
     * The implementation may include actions like checking for changes, applying updates,
     * and preparing synchronization data as needed.
     */
    void tickServer();

    /**
     * Checks whether the associated data or state has been modified since the last reset or synchronization.
     *
     * @return true if the data or state has changed, false otherwise.
     */
    boolean hasChanged();

    /**
     * Marks the associated data or state as changed manually. This indicates that the data
     * has been modified and may require actions such as synchronization or further
     * processing during the next update cycle.
     */
    void markAsChanged();

    /**
     * Resets the changed state for the associated data or binding.
     * This method is typically invoked after the changes have been processed
     * or synchronized, ensuring that the state reflects no pending updates or modifications.
     */
    void clearChanged();

    /**
     * Writes server-to-client (S2C) synchronization data to the provided buffer.
     * This method serializes the necessary data to ensure the client remains in sync
     * with the server's current state.
     *
     * @param buffer the buffer into which the S2C synchronization data will be written
     */
    void writeS2CSyncData(RegistryFriendlyByteBuf buffer);

    /**
     * Reads client-to-server (C2S) synchronization data from the provided buffer.
     * This method deserializes and updates the internal state based on the incoming data from the client.
     * Ensure that the data conforms to the expected format and follows the defined synchronization protocol.
     * You should always check {@link #acceptC2S()} ()} before calling this method to avoid unexpected updates, e.g., attacks
     *
     *
     * @param buffer the buffer containing the C2S synchronization data to be read
     */
    void readC2SSyncData(RegistryFriendlyByteBuf buffer);

    /**
     * Determines whether data synchronization is required from the server to the client (S2C).
     * The necessity for synchronization is based on the synchronization strategy defined
     * by {@code getS2CStrategy()} and the change state of the associated data as indicated by {@code hasChanged()}.
     *
     * @return {@code true} if server-to-client synchronization is needed, {@code false} otherwise.
     */
    default boolean needS2CSync() {
        var strategy = getS2CStrategy();
        return strategy == SyncStrategy.ALWAYS || hasChanged() && strategy != SyncStrategy.NONE;
    }
}
