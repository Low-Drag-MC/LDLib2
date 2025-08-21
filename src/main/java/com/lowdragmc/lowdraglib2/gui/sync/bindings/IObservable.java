package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import javax.annotation.ParametersAreNonnullByDefault;

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
}
