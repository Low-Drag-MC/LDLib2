package com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable;

public enum ModifierFlags {
    /**
     * The variable is neither readable nor writable.
     */
    NONE(0),
    /**
     * The variable can be read from.
     */
    READ(1),
    /**
     * The variable can be written to.
     */
    WRITE(1 << 1),
    /**
     * The variable can be read from and written to.
     */
    READ_WRITE(1 | 1 << 1);

    public final int mask;

    ModifierFlags(int mask) {
        this.mask = mask;
    }

    public boolean hasFlag(ModifierFlags flag) {
        return (mask & flag.mask) != 0;
    }
}
