package com.lowdragmc.lowdraglib.syncdata.managed;


public interface IDirectVar<T> {
    /**
     * Get the internal value.
     */
    T value();

    /**
     * Set the internal value.
     */
    void set(T value);

    /**
     * Check if the type is primitive.
     * Internal value cannot be null if the type is primitive.
     */
    default boolean isPrimitive() {
        return getType().isPrimitive();
    }

    /**
     * Get the type of the internal value.
     */
    Class<T> getType();
}
