package com.lowdragmc.lowdraglib.gui.ui.data;

/**
 * Enum representing the vertical alignment.
 */
public enum Vertical {
    TOP(0, "top"),
    CENTER(0.5f, "center"),
    BOTTOM(1, "bottom");

    public final float offset;
    public final String name;

    Vertical(float offset, String name) {
        this.offset = offset;
        this.name = name;
    }
}
