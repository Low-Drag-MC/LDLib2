package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

public enum PortModelOptions {
    NONE(0),
    NO_EMBEDDED_CONSTANT(1),
    HIDDEN(1 << 1),
    NODE_OPTION(1 << 2);

    public final int mask;

    PortModelOptions(int mask) {
        this.mask = mask;
    }

    public boolean hasFlag(PortModelOptions flag) {
        return (mask & flag.mask) != 0;
    }
}
