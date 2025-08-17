package com.lowdragmc.lowdraglib2.gui.sync.bindings;

public interface IDataSource<T> {

    void setValueWithoutNotify(T value);

    /**
     * Notify changes actively.
     */
    void notifyChange();

    /**
     * Set the value of the data source.
     * @param value the new value to set
     */
    default void setValue(T value, boolean notify) {
        setValueWithoutNotify(value);
        if (notify) {
            notifyChange();
        }
    }

    default void setValue(T value) {
        setValue(value, true);
    }
}
