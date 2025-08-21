package com.lowdragmc.lowdraglib2.gui.sync.bindings;

public interface IObserver<T> {
    /**
     * Set the value of the data source.
     * @param value the new value to set
     */
    void setValue(T value, boolean notify);

    default void setValue(T value) {
        setValue(value, true);
    }
}
