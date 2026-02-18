package com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable;

import com.mojang.serialization.DataResult;

import java.lang.reflect.Type;

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
     * Gets the data type of the variable.
     *
     * @return the data type class
     */
    Type getDataType();

    /**
     * Gets the kind of the variable, such as Local, Input, or Output.
     */
    VariableKind getVariableKind();

    /**
     * Tries to retrieve the default value of the variable.
     * @param expectedType The expected type of the default value.
     * @return return True if the default value is present and can be cast to the expected type. otherwise return false.
     */
    <T> DataResult<T> tryGetDefaultValue(Type expectedType);
}
