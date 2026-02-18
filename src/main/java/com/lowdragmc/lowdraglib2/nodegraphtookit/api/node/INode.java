package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPort;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface INode {
    AbstractNodeModel getNodeModel();

    /**
     * Retrieves an input port using its index.
     *
     * <p>The index is zero-based. The list of input ports is ordered according to their display order in the node.</p>
     *
     * @param index the index of the input port (zero-based)
     * @return the input port at the specified index
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    @Nullable
    default IPort getInputPort(int index) {
        if (getNodeModel() instanceof NodeModel nodeModel) return nodeModel.getInputsByDisplayOrder().get(index);
        return null;
    }

    /**
     * Retrieves all input ports on the node in the order they are displayed.
     *
     * @return an iterable view of input ports in display order
     */
    default Collection<? extends IPort> getInputPorts() {
        if (getNodeModel() instanceof NodeModel nodeModel) return nodeModel.getInputsByDisplayOrder();
        return List.of();
    }

    /**
     * Retrieves an input port using its name.
     *
     * <p>The input port's name is unique within the node's input ports and node options.</p>
     *
     * @param name the unique name of the input port within this node
     * @return the input port with the specified name
     * @throws java.util.NoSuchElementException if no port matches the name (depending on backing map behavior)
     * @throws NullPointerException if {@code name} is null
     */
    @Nullable
    default IPort getInputPortById(String name) {
        if (getNodeModel() instanceof NodeModel nodeModel) return nodeModel.getInputsById().get(name);
        return null;
    }

    /**
     * Retrieves an output port using its index in the displayed order.
     *
     * <p>The index is zero-based. The list of output ports is ordered according to their display order in the node.</p>
     *
     * @param index the zero-based index of the output port
     * @return the output port at the specified index
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    @Nullable
    default IPort getOutputPort(int index) {
        if (getNodeModel() instanceof NodeModel nodeModel) return nodeModel.getOutputsByDisplayOrder().get(index);
        return null;
    }

    /**
     * Retrieves all output ports on the node in the order they are displayed.
     *
     * @return an iterable view of output ports in display order
     */
    default Collection<? extends IPort> getOutputPorts() {
        if (getNodeModel() instanceof NodeModel nodeModel) return nodeModel.getOutputsByDisplayOrder();
        return List.of();
    }

    /**
     * Retrieves an output port using its name.
     *
     * <p>The output port's name is unique within the node's output ports.</p>
     *
     * @param name the unique name of the output port within this node
     * @return the output port with the specified name, or {@code null} if no match is found
     * @throws NullPointerException if {@code name} is null
     */
    @Nullable
    default IPort getOutputPortById(String name) {
        if (getNodeModel() instanceof NodeModel nodeModel) return nodeModel.getOutputsById().get(name);
        return null;
    }

    /**
     * Gets the list of node options defined on this node.
     *
     * @return the list of node options
     */
    @Nullable
    default Collection<? extends INodeOption> getNodeOptions() {
        if (getNodeModel() instanceof NodeModel nodeModel) return nodeModel.getNodeOptions();
        return null;
    }

    /**
     * Gets a node option by its unique name.
     *
     * @param id the unique id of the option
     * @return the node option, or {@code null} if not found
     */
    @Nullable
    default INodeOption getNodeOptionById(String id) {
        if (getNodeModel() instanceof NodeModel nodeModel) return nodeModel.getNodeOptionById(id);
        return null;
    }
}
