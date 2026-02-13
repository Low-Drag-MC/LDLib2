package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency;

public enum DependencyTypes {
    NONE(0),
    STYLE(1),
    LAYOUT(1 << 1),
    REMOVAL(1 << 2),
    ANY(0xFFFFFFFF);
    ;
    public final int mask;

    DependencyTypes(int mask) {
        this.mask = mask;
    }

    public boolean hasFlag(DependencyTypes flag) {
        return (mask & flag.mask) != 0;
    }
}
