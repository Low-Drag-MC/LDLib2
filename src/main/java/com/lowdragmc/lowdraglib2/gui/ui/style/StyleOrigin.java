package com.lowdragmc.lowdraglib2.gui.ui.style;

/**
 * Represents the origin of a style or CSS rule applied to a UI component.
 * This enum provides information about the priority and source of the style,
 * which helps in determining how styling is handled or overridden.
 */
public enum StyleOrigin {
    /**
     * default style (ui component internal)
     */
    DEFAULT(0),
    /**
     * external (Stylesheet)
     */
    STYLESHEET(2),
    /**
     * internal（setStyle）
     */
    INLINE(3),
    ANIMATION(4),
    IMPORTANT(5),
    ;
    public final int priority;

    StyleOrigin(int priority) {
        this.priority = priority;
    }
}