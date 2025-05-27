package com.lowdragmc.lowdraglib.gui.ui.bindings;

public interface IObservable<T> {
    /**
     * Bind an observer to this observable.
     * The observer will be notified of changes to the value.
     *
     * @param observer the observer to bind
     */
    void bindObserver(IObserver<T> observer);
}
