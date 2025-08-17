package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IObservable<T> {
    /**
     * Bind an observer to this observable.
     * The observer will be notified of changes to the value.
     *
     * @param observer the observer to bind
     */
    void bindObserver(IObserver<T> observer);

    /**
     * Unbinds an observer from this observable. Once unbound, the observer will no longer
     * receive notifications about changes to the value.
     *
     * @param observer the observer to unbind
     */
    void unbindObserver(IObserver<T> observer);
}
