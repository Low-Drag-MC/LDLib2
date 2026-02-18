package com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable;

public enum VariableKind {
    /**
     * A variable used only within the current graph.
     */
    LOCAL,

    /**
     * A variable used as an input to a subgraph.
     * <br>
     * This kind exposes the variable to the parent graph when the graph is used as a subgraph.
     * The parent graph can provide a value for it.
     */
    INPUT,

    /**
     * A variable used as an output to a subgraph.
     * <br>
     * This kind exposes the variable to the parent graph when the graph is used as a subgraph.
     * The subgraph assigns a value that the parent graph can read.
     */
    OUTPUT;
}
