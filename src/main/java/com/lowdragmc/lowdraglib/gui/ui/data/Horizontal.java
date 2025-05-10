package com.lowdragmc.lowdraglib.gui.ui.data;

/**
 * Enum representing the horizontal alignment.
 */
public enum Horizontal {
    LEFT(0, "left"),
    CENTER(0.5f, "center"),
    RIGHT(1, "right");

    public final float offset;
    public final String name;

    Horizontal(float offset, String name) {
        this.offset = offset;
        this.name = name;
    }
}
