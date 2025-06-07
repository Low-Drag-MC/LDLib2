package com.lowdragmc.lowdraglib2.gui.ui.bindings;

public interface IDataSourceProvider<T> {
    /**
     * bind a data source to this provider.
     */
    void bindDataSource(IDataSource<T> dataSource);
}
