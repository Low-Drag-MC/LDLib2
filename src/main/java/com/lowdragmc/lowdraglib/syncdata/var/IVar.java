package com.lowdragmc.lowdraglib.syncdata.var;

/**
 * Var is an interface to access and modify a value.
 */
public interface IVar<TYPE> {
    /**
     * Get the internal value.
     */
    TYPE value();

    /**
     * Set the internal value.
     */
    void set(TYPE value);

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
    Class<TYPE> getType();
}
