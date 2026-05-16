package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

public interface IHasElementColor {
    /**
     * Get the color of the element.
     */
    int getElementColor();

    /**
     * Set the color of the element.
     */
    void setColor(int color);

    /**
     * Get the default color of the element.
     */
    int getDefaultColor();

    /**
     * Whether the color has been set by the user.
     */
    boolean hasUserColor();

    /**
     * Reverts to the default color and clears the user-set flag. The default implementation
     * calls {@link #setColor} with {@link #getDefaultColor()}, which leaves {@link #hasUserColor}
     * returning {@code true} — concrete implementors should override this to also clear the
     * user-color flag so the colored decoration (e.g. the title color bar on nodes) goes back
     * to its unset state.
     */
    default void resetColor() {
        setColor(getDefaultColor());
    }
}
