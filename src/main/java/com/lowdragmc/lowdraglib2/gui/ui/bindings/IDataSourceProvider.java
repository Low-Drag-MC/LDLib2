package com.lowdragmc.lowdraglib2.gui.ui.bindings;

public interface IDataSourceProvider<T> {
    /**
     * bind a data source to this provider.
     */
    void bindDataSource(IDataSource<T> dataSource);

    /**
     * Unbinds a data source from this provider. After unbinding, the data source
     * will no longer be associated with this provider and will not receive or provide updates.
     *
     * @param dataSource the data source to unbind from this provider
     */
    void unbindDataSource(IDataSource<T> dataSource);
}
