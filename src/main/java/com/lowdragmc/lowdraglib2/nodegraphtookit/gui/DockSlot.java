package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper.ResizeHandle;

import java.util.EnumSet;
import java.util.Set;

public enum DockSlot {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    /** Free floating: arbitrary position, all 8 resize handles allowed. */
    CENTER;

    public boolean isCorner() {
        return this != CENTER;
    }

    public Set<ResizeHandle> allowedResizeHandles() {
        return switch (this) {
            case TOP_LEFT     -> EnumSet.of(ResizeHandle.RIGHT, ResizeHandle.BOTTOM, ResizeHandle.BOTTOM_RIGHT);
            case TOP_RIGHT    -> EnumSet.of(ResizeHandle.LEFT,  ResizeHandle.BOTTOM, ResizeHandle.BOTTOM_LEFT);
            case BOTTOM_LEFT  -> EnumSet.of(ResizeHandle.RIGHT, ResizeHandle.TOP, ResizeHandle.TOP_RIGHT);
            case BOTTOM_RIGHT -> EnumSet.of(ResizeHandle.LEFT,  ResizeHandle.TOP, ResizeHandle.TOP_LEFT);
            case CENTER       -> EnumSet.allOf(ResizeHandle.class);
        };
    }
}
