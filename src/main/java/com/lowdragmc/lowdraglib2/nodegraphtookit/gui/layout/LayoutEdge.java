package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

/**
 * A connection between two {@link LayoutBox boxes}, by {@link LayoutBox#id}.
 *
 * <p>The two offsets are what make a layered result look like a wiring diagram instead of a row of
 * centred rectangles: they say where along the box's cross axis the wire actually attaches, so
 * coordinate assignment can line up the <em>ports</em> rather than the boxes. A node whose output
 * port is near its bottom edge gets pulled up accordingly.</p>
 *
 * @param fromOffset cross-axis distance from the source box's leading edge to the port the wire
 *                   leaves. {@link Float#NaN} means "unknown", and is read as the box's centre.
 * @param toOffset   the same for the port the wire arrives at.
 */
public record LayoutEdge(int from, int to, float fromOffset, float toOffset) {

    /** An edge with no port information — both ends are treated as attaching at the box centre. */
    public LayoutEdge(int from, int to) {
        this(from, to, Float.NaN, Float.NaN);
    }

    /** This edge with its direction flipped, offsets following their ends. */
    public LayoutEdge reversed() {
        return new LayoutEdge(to, from, toOffset, fromOffset);
    }

    /** Resolves {@link #fromOffset} against the box it belongs to, defaulting to the centre. */
    public float resolveFromOffset(LayoutBox box, LayoutDirection direction) {
        return resolve(fromOffset, box, direction);
    }

    /** Resolves {@link #toOffset} against the box it belongs to, defaulting to the centre. */
    public float resolveToOffset(LayoutBox box, LayoutDirection direction) {
        return resolve(toOffset, box, direction);
    }

    private static float resolve(float offset, LayoutBox box, LayoutDirection direction) {
        return Float.isNaN(offset) ? box.crossSize(direction) / 2f : offset;
    }
}
