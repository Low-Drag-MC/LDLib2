package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;

@ParametersAreNonnullByDefault
public interface IObservable<T> {
    /**
     * bind an observer to it.
     */
    UIElement bindObserver(IObserver<T> observer);

    /**
     * Unbinds a data observer from it. After unbinding, the observer
     * will no longer be associated with this and will not receive or provide updates.
     *
     * @param observer the data source to unbind from it
     */
    UIElement unbindObserver(IObserver<T> observer);

    /**
     * Retrieves a collection of observers currently bound to this observable instance.
     * This method provides access to all observers that are linked and actively receiving updates.
     *
     * @return a collection of {@link IObserver} instances bound to this observable.
     */
    @ApiStatus.Internal
    default Collection<IObserver<T>> getBoundObservers() {
        return Collections.emptyList();
    }
}
