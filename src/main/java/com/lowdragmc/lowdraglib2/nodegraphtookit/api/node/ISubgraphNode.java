package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;

/**
 * Interface for a specialized node that references a subgraph and exposes its input and output variables as ports.
 *
 * <p>Subgraph nodes act as entry points to reusable graphs. These nodes mirror the subgraph's inputs and outputs
 * as ports on the node to allow the integration of subgraphs within a main graph.
 * The subgraph must be a valid {@link Graph} type. The main graph must support subgraphs through
 * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphOptions#SUPPORTS_SUBGRAPHS},
 * and the subgraph must be linked to the main graph using the
 * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.SubgraphAttribute}.</p>
 */
public interface ISubgraphNode extends INode {

    /**
     * Retrieves the subgraph linked to this node.
     *
     * <p>Call this method to access the subgraph that provides the behavior for this node.
     * The subgraph defines input and output variables that appear as ports on the subgraph node.
     * This method does not create or modify the subgraph.</p>
     *
     * @return the {@link Graph} instance that this node references
     */
    Graph getSubgraph();
}
