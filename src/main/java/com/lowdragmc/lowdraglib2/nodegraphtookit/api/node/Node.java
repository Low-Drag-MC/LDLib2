package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPort;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import lombok.Getter;
import net.minecraft.network.chat.Component;

/**
 * The base class for all user-accessible nodes in a graph.
 *
 * <p>Inherit from this class to define custom node types that appear in the graph. The {@link Node} class provides
 * lifecycle hooks, serialization support, and the structure needed to define ports, UI behaviors, and custom logic.
 * This class forms the foundation of all user-defined nodes in a graph-based tool, including variable nodes,
 * context nodes, and subgraph nodes.</p>
 *
 * <p>To create a custom node, derive from {@link Node}, define its input and output ports using a port builder
 * in {@link #onDefinePorts(IPortDefinitionContext)}, and define its options in
 * {@link #onDefineOptions(IOptionDefinitionContext)}.</p>
 *
 * <p>This class is used in combination with other types like {@link INode}, {@link IPort}, and
 * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph} to construct and manage node-based workflows.</p>
 *
 * @see INode
 * @see IVariableNode
 * @see ISubgraphNode
 */
public abstract class Node implements INode {
    /**
     * Backing implementation model.
     */
    @Getter
    private AbstractNodeModel nodeModel;

    public void setImplementation(NodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public abstract Component getDisplayName();

    public IGuiTexture getNodeIcon() {
        return Icons.NODE;
    }

    /**
     * Defines the structure of the node by building its ports and options.
     *
     * <p>This method calls both {@link #onDefineOptions(IOptionDefinitionContext)} and
     * {@link #onDefinePorts(IPortDefinitionContext)} to allow custom definition of the node.</p>
     */
    public void defineNode() {
        if (nodeModel instanceof NodeModel n) {
            n.defineNode();
        }
    }

                                 /**
     * Called during {@link #defineNode()} to define the options available on the node.
     *
     * <p>This method is called before {@link #onDefinePorts(IPortDefinitionContext)}. Override this method to add node options
     * using the provided {@link IOptionDefinitionContext}.</p>
     *
     * @param context provides methods for defining node options
     */
    public void onDefineOptions(IOptionDefinitionContext context) {}

    /**
     * Called during {@link #defineNode()} to define the input and output ports of the node.
     *
     * <p>This method is called after {@link #onDefineOptions(IOptionDefinitionContext)} and is used to declare the structure
     * of the node's connectivity. Use the provided {@link IPortDefinitionContext} to define input and output ports using a
     * builder pattern.</p>
     *
     * @param context provides methods for defining input and output ports
     */
    public void onDefinePorts(IPortDefinitionContext context) {}

}
