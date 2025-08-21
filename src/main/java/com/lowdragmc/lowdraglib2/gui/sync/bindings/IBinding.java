package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import com.lowdragmc.lowdraglib2.gui.sync.SyncValue;

import javax.annotation.Nullable;

public interface IBinding<T> {
    /**
     * Get the strategy for synchronizing data from client to server.
     */
    SyncStrategy c2sStrategy();

    /**
     * Determines whether the binding accepts synchronization data from server to client (S2C).
     *
     * @return true if this binding allows S2C synchronization data, false otherwise.
     */
    default boolean acceptS2C() {
        return s2cStrategy().doSync();
    }

    /**
     * Get the strategy for synchronizing data from server to client.
     */
    SyncStrategy s2cStrategy();

    /**
     * Determines whether synchronization data from client to server (C2S) is accepted.
     *
     * @return true if the data source or binding accepts C2S synchronization; false otherwise.
     */
    default boolean acceptC2S() {
        return c2sStrategy().doSync();
    }

    /**
     * Retrieves the {@code SyncValue} associated with this binding.
     * The {@code SyncValue} represents the core value holder for data synchronization
     * and provides mechanisms to manage and track changes to the data.
     *
     * @return the {@link SyncValue} instance associated with this binding.
     */
    SyncValue<T> getSyncValue();

    /**
     * Sets the remote data source for this binding. The remote data source provides
     * the mechanism to retrieve or update data for synchronization purposes.
     *
     * @param dataSource the {@link IDataSource} to set as the remote data source;
     *                   can be {@code null} to clear the current remote data source.
     */
    void setRemoteDataSource(@Nullable IDataSource<T> dataSource);

    /**
     * Sets the server-side data source used for synchronization with this binding.
     * The data source provides a mechanism for retrieving or updating data for the server.
     *
     * @param dataSource the {@link IDataSource} instance to set as the server-side data source;
     *                   can be {@code null} to unset or clear the current server-side data source.
     */
    void setServerDataSource(@Nullable IDataSource<T> dataSource);
}
