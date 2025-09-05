package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IDataConsumer<T> {
    /**
     * Bind a dataProvider to this observable.
     * The dataProvider will be notified of changes to the value.
     *
     * @param dataProvider the dataProvider to bind
     */
    UIElement bindDataSource(IDataProvider<T> dataProvider);

    /**
     * Unbinds a dataProvider from this observable. Once unbound, the dataProvider will no longer
     * receive notifications about changes to the value.
     *
     * @param dataProvider the dataProvider to unbind
     */
    UIElement unbindDataSource(IDataProvider<T> dataProvider);
}
