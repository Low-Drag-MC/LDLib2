package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.snap;

/**
 * What the snapper is allowed to do, in canvas content units.
 *
 * <p>Both distances are handed in already divided by the canvas zoom, so that a snap "feels" the
 * same size wherever the user is zoomed to: eight screen pixels is eight screen pixels whether the
 * canvas is at 2x or at a quarter.</p>
 *
 * @param grid           whether to fall back to the grid when nothing else is in reach
 * @param gridSize       grid pitch; ignored when {@code grid} is false or this is not positive
 * @param align          whether to line up with other elements at all
 * @param alignThreshold how close an edge has to be before it grabs
 * @param searchRange    how far away, measured across the guide, an element may be and still count
 *                       as "nearby" — a node on the far side of the canvas is not something the
 *                       user is trying to line up with
 */
public record SnapSettings(boolean grid, float gridSize, boolean align, float alignThreshold, float searchRange) {

    /** Neither grid nor alignment: the drag is left exactly where the cursor put it. */
    public static final SnapSettings NONE = new SnapSettings(false, 0, false, 0, 0);

    public static SnapSettings gridOnly(float gridSize) {
        return new SnapSettings(true, gridSize, false, 0, 0);
    }
}
