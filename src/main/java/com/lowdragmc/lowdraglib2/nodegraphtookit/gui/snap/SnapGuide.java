package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.snap;

/**
 * A line the drag lined up with, in canvas content coordinates — what gets drawn so the user can
 * see <em>which</em> edge they landed on rather than only that something moved.
 *
 * @param vertical {@code true} for a line at a fixed x, running up and down
 * @param position the fixed coordinate: x for a vertical guide, y for a horizontal one
 * @param start    where the drawn span begins on the other axis
 * @param end      where it ends; the span covers the dragged rectangle and every element it lined
 *                 up with, so three left-aligned nodes get one guide through all of them
 * @param anchor   which edge (or centre) of the dragged rectangle landed here
 */
public record SnapGuide(boolean vertical, float position, float start, float end, SnapAnchor anchor) {
}
