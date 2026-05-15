package com.lowdragmc.lowdraglib2.gui.ui.data;

public enum Clip {
    /**
     * No clipping.
     */
    NONE,
    /**
     * Clip the element to the content bounds.
     */
    SCISSOR,
    /**
     * Mask the element with the given mask {@link com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry#MASK}.
     */
    MASK,
    /**
     * Mask the element with the given mask dynamically (update mask every frame).
     */
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
