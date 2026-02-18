package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.IVariable;

import java.util.List;

public interface IGraph {
    /**
     * @return variable models in creation order
     */
    List<? extends IVariable> getVariables();

    /**
     * @return nodes in creation order
     */
    List<? extends INode> getNodes();
}
