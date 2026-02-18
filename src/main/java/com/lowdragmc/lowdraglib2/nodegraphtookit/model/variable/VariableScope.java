package com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable;

public enum VariableScope {
    UNKNOWN,
    /**
     * Local scope, used only within the graph
     */
    LOCAL,

    /**
     * Exposed scope, settable through the global inspector.
     */
    EXPOSED
}
