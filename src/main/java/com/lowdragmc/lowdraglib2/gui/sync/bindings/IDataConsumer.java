package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IDataConsumer<T> {
    /**
     * Bind a dataSource to this observable.
     * The dataSource will be notified of changes to the value.
     *
     * @param dataSource the dataSource to bind
     */
    UIElement bindDataSource(IDataProvider<T> dataSource);

    /**
     * Unbinds a dataSource from this observable. Once unbound, the dataSource will no longer
     * receive notifications about changes to the value.
     *
     * @param dataSource the dataSource to unbind
     */
    UIElement unbindDataSource(IDataProvider<T> dataSource);
}
