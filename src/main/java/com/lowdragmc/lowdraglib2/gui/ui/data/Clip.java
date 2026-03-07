package com.lowdragmc.lowdraglib2.gui.ui.data;

public enum Clip {
    NONE,
    SCISSOR,
    MASK;

    public boolean isClip() {
        return this != NONE;
    }

    public boolean isMask() {
        return this == MASK;
    }

    public boolean isScissor() {
        return this == SCISSOR;
    }
}
