package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

/**
 * Hints about what aspect of a model has changed.
 *
 * <p>Used to optimize UI updates by indicating which parts of a model need to be refreshed.</p>
 */
public enum ChangeHint {
    /**
     * Unspecified changes. Assume anything could have change.
     */
    UNSPECIFIED(1),

    /**
     * The data/value of the model has changed.
     */
    DATA(1 << 1),

    /**
     * The layout/position of the model has changed.
     */
    LAYOUT(1 << 2),

    /**
     * The visual style (color, etc.) of the element changed.
     */
    STYLE(1 << 3),

    /**
     * Graph topology changed; typically, a wire was connected or disconnected.
     */
    GRAPH_TOPOLOGY(1 << 4),

    /**
     * Grouping of variable in the blackboard changed.
     */
    GROUPING(1 << 5)
    ;
    public final int mask;

    ChangeHint(int mask) {
        this.mask = mask;
    }
}
