package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

/**
 * Enumeration representing the type of a port.
 */
public enum PortType {
    /**
     * The port is used for the graph flow.
     */
    DEFAULT,

    /**
     * The port is used as a connection point for transitions in state machines.
     */
    STATE,

    /**
     * A missing port placeholder for ports that were removed but still have connections.
     */
    MISSING_PORT
}
