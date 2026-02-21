package com.lowdragmc.lowdraglib2.gui.sync.bindings;

@FunctionalInterface
public interface IObserver<T> {
    /**
     * Set the value of the data source.
     * @param value the new value to set
     */
    void onValueChanged(T value);
}
