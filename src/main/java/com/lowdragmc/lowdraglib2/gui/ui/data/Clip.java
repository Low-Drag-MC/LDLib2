package com.lowdragmc.lowdraglib2.gui.ui.data;

public enum Clip {
    NONE,
    SCISSOR,
    MASK,
    DYNAMIC_MASK;

    public boolean isClip() {
        return this != NONE;
    }

    public boolean isMask() {
        return this == MASK || this == DYNAMIC_MASK;
    }

    public boolean isDynamicMask() {
        return this == DYNAMIC_MASK;
    }

    public boolean isScissor() {
        return this == SCISSOR;
    }
}
