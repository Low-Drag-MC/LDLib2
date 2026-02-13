package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INode;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface IGraph {
    /**
     * @return variable models in creation order
     */
    List<IVariable> getVariables();

    /**
     * @return nodes in creation order
     */
    List<INode> getNodes();
}
