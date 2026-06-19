package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;

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

    /**
     * Retrieves a collection of data providers (data sources) that are currently bound to this instance.
     * Bound data providers are notified of any changes to the associated value.
     *
     * @return a collection of {@link IDataProvider} instances that are currently bound.
     *         If no data providers are bound, an empty collection is returned.
     */
    @ApiStatus.Internal
    default Collection<IDataProvider<T>> getBoundDataSources() {
        return Collections.emptyList();
    }
}
