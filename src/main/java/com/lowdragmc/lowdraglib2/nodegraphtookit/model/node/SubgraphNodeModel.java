package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;

public class SubgraphNodeModel extends NodeModel {
    // todo SubgraphNodeModel
    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope) {
    }


    public boolean isReferencingLocalSubgraph() {
        return false;
    }

    public GraphModel getSubgraphModel() {
        return null;
    }
}
