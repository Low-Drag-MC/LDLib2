package com.lowdragmc.lowdraglib2.gui.ui.style.value;

/**
 * Enum representing the text wrapping style.
 */
public enum TextWrap {
    /**
     * No wrapping. The text will be displayed in a single line.
     */
    NONE,
    /**
     * Wrap the text to the next line when it reaches the end of the container.
     */
    WRAP,
    /**
     * Wrap the text if it exceeds the container width and roll to the next line over the time.
     */
    ROLL,
    /**
     * Wrap the text if it exceeds the container width and roll to the next line when hovered.
     */
    HOVER_ROLL,
    /**
     * Discard the text if it exceeds the container width.
     */
    HIDE,
}
