package com.lowdragmc.lowdraglib.gui.ui.bindings;

public interface IDataSourceProvider<T> {
    /**
     * bind a data source to this provider.
     */
    void bindDataSource(IDataSource<T> dataSource);
}
