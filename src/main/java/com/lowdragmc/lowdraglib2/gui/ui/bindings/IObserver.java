package com.lowdragmc.lowdraglib2.gui.ui.bindings;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;

import java.util.function.Consumer;

public interface IObserver<T> {
    /**
     * Register a listener to observe changes in the value.
     * @param listener the listener to register, which will be called with the new value when it changes.
     * @return an ISubscription that can be used to unsubscribe the listener.
     */
    ISubscription registerListener(Consumer<T> listener);

    /**
     * Get the current value of the observer.
     * @return the current value.
     */
    T getValue();

    /**
     * Set a new value for the observer.
     * @param callImmediately if true, the listener will be called immediately with the new value.
     */
    default ISubscription registerListener(Consumer<T> listener, boolean callImmediately) {
        var subscription = registerListener(listener);
        if (callImmediately) {
            listener.accept(getValue());
        }
        return subscription;
    }
}
