package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

import java.util.function.Supplier;

/**
 * The arrangement strategies offered in the graph's contextual menu.
 *
 * <p>Ordered by how often they are the right answer. {@link #LAYERED} is the one to reach for on a
 * dataflow graph; the other two exist because it is a bad fit for exactly two situations — a graph
 * with no flow to find, and a pile of nodes with no wires at all.</p>
 */
public enum GraphLayoutAlgorithm {
    /** Sugiyama layered drawing — see {@link LayeredLayout}. */
    LAYERED("graph.layout.layered", LayeredLayout::new),
    /** Even grid, wires ignored — see {@link GridLayout}. */
    GRID("graph.layout.grid", GridLayout::new),
    /**
     * Spring embedder — see {@link ForceDirectedLayout}. Wrapped per component, because nothing in
     * a spring model holds two unconnected clusters together; see {@link ComponentwiseLayout}.
     */
    FORCE_DIRECTED("graph.layout.force_directed",
            () -> new ComponentwiseLayout(new ForceDirectedLayout()));

    private final String translationKey;
    private final Supplier<IGraphLayout> factory;

    GraphLayoutAlgorithm(String translationKey, Supplier<IGraphLayout> factory) {
        this.translationKey = translationKey;
        this.factory = factory;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public IGraphLayout create() {
        return factory.get();
    }
}
