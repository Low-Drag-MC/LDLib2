package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.snap;

import org.joml.Vector4f;

/**
 * One of the three places along an axis a rectangle can be lined up by.
 *
 * <p>Having all three is the whole difference between the old snapping and this one: rounding only
 * the top-left corner means a node's <em>right</em> edge lands on a grid line only by accident, and
 * two nodes of different widths can never be made to line up on the right at all.</p>
 */
public enum SnapAnchor {
    /** Left edge on the x axis, top edge on the y axis. */
    START,
    /** Right edge on the x axis, bottom edge on the y axis. */
    END,
    /** Horizontal or vertical centre. */
    CENTER;

    /**
     * Where this anchor sits on the given rectangle. Rectangles are {@code (x, y, width, height)}.
     *
     * @param horizontal {@code true} for the x axis
     */
    public float of(Vector4f rect, boolean horizontal) {
        var start = horizontal ? rect.x : rect.y;
        var size = horizontal ? rect.z : rect.w;
        return switch (this) {
            case START -> start;
            case END -> start + size;
            case CENTER -> start + size / 2f;
        };
    }
}
