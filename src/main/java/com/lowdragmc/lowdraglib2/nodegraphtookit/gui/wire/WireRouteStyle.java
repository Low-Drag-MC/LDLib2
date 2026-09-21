package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wire;

/**
 * How a wire gets from one port to the other. A view-wide setting, like snap-to-grid — mixing
 * styles inside one graph reads as a bug rather than as intent.
 */
public enum WireRouteStyle {
    /**
     * The original look: a straight run between the two ports with a short tangent stub at each
     * end, corners filleted.
     */
    DEFAULT("graph.wire_style.default", 6f, 8),
    /**
     * A cubic Bézier leaving each port along the port's own axis — the curve Blender, Unreal's
     * Blueprint editor and the web node editors draw. Already smooth, so it takes no fillet.
     */
    CURVED("graph.wire_style.curved", 0f, 0),
    /**
     * Horizontal trunk plus 45° chamfers — the Unreal Blueprint / shader-editor style. A wire runs
     * along the flow axis and cuts across at 45° where it needs to change lanes, so every segment
     * is either axis-aligned or a true diagonal. Also called chamfered or diagonal-corner routing.
     */
    OCTILINEAR("graph.wire_style.octilinear", 3f, 3),
    /**
     * The same route with square corners instead of chamfered ones: axis-aligned segments only,
     * rounded where they meet. Closer to a schematic or a flowchart.
     */
    ORTHOGONAL("graph.wire_style.orthogonal", 8f, 8),
    /** Port to port, no stubs and no detours. */
    STRAIGHT("graph.wire_style.straight", 6f, 8);

    private final String translationKey;
    private final float cornerRadius;
    private final int cornerSegments;

    WireRouteStyle(String translationKey, float cornerRadius, int cornerSegments) {
        this.translationKey = translationKey;
        this.cornerRadius = cornerRadius;
        this.cornerSegments = cornerSegments;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Fillet radius applied to the finished polyline, or {@code 0} to leave it alone. Octilinear
     * keeps it small — the 45° cut is the shape, and rounding it away would turn the style back
     * into the default one — but not zero, because the wire is drawn as a wide textured strip and a
     * hard corner notches it.
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    /** Segments the fillet is sampled at; each one costs a vertex per corner. */
    public int getCornerSegments() {
        return cornerSegments;
    }

    /** Whether this style routes through {@link WireRouter} rather than drawing the waypoints directly. */
    public boolean isRouted() {
        return this == CURVED || this == OCTILINEAR || this == ORTHOGONAL;
    }
}
