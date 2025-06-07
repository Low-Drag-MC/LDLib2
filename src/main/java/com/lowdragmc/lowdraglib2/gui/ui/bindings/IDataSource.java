package com.lowdragmc.lowdraglib2.gui.ui.bindings;

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
    default void setValue(T value) {
        setValueWithoutNotify(value);
        notifyChange();
    }
}
