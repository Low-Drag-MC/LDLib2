package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

import java.util.List;

/**
 * An arrangement strategy. Implementations are pure geometry — no models, no UI, no Minecraft — so
 * they can be unit tested directly.
 */
public interface IGraphLayout {

    /**
     * Rewrites every box's {@link LayoutBox#x}/{@link LayoutBox#y} in place.
     *
     * <p>The result is deliberately un-anchored: it is laid out around whatever origin the algorithm
     * finds convenient, and the caller translates the whole thing to keep the selection where the
     * user left it. Edges whose endpoints are out of range, or that connect a box to itself, are
     * ignored rather than rejected — a graph view hands over whatever it has.</p>
     */
    void layout(List<LayoutBox> boxes, List<LayoutEdge> edges, LayoutOptions options);
}
