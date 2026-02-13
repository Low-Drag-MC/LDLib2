package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

/**
 * Interface for a variable declared in a graph.
 *
 * <p>Variables are declarations displayed in the graph's Blackboard. They can be referenced
 * by variable nodes in the graph. Each variable has a name, data type, and optional default value.</p>
 */
public interface IVariable {
    /**
     * Gets the unique name of the variable.
     *
     * @return the variable name
     */
    String getName();

    /**
     * Gets the display name of the variable shown in the UI.
     *
     * @return the display name
     */
    String getDisplayName();

    /**
     * Gets the data type of the variable.
     *
     * @return the data type class
     */
    Class<?> getDataType();

    /**
     * Attempts to retrieve the current value of the variable.
     *
     * @param <T> the expected type of the value
     * @param type the class of the expected type
     * @return the value if available and type matches, or null
     */
    <T> T getValue(Class<T> type);

    /**
     * Sets the value of the variable.
     *
     * @param value the new value
     */
    void setValue(Object value);
}
