package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

public enum SpawnFlags {
    NONE(0),
    RESERVED0(1),
    RESERVED1(1 << 1),
    ORPHAN(1 << 2),
    DEFAULT(0);

    public final int bit;

    SpawnFlags(int bit) {
        this.bit = bit;
    }

    public boolean isOrphan() {
        return (bit & ORPHAN.bit) != 0;
    }
}
