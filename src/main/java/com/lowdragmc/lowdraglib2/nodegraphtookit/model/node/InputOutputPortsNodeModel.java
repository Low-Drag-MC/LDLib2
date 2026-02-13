package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.*;

import java.util.*;

/**
 * Node model that supports both input and output ports, as well as node options.
 *
 * <p>This is the base class for most node types that have ports on both sides.</p>
 */
public abstract class InputOutputPortsNodeModel extends PortNodeModel {
    protected final List<NodeOption> nodeOptions = new ArrayList<>();
    protected final Map<String, NodeOption> nodeOptionsById = new HashMap<>();

    @Override
    public Collection<PortModel> getPorts() {
        var inputPorts = getInputPorts();
        var outputPorts = getOutputPorts();
        List<PortModel> allPorts = new ArrayList<>(inputPorts.size() + outputPorts.size());
        allPorts.addAll(inputPorts);
        allPorts.addAll(outputPorts);
        return allPorts;
    }

    public abstract Map<String, PortModel> getInputsById();

    public abstract Map<String, PortModel> getOutputsById();

    /**
     * Gets the input ports ordered by display order.
     *
     * @return the input ports
     */
    public abstract List<PortModel> getInputsByDisplayOrder();

    /**
     * Gets the output ports ordered by display order.
     *
     * @return the output ports
     */
    public abstract List<PortModel> getOutputsByDisplayOrder();

    public List<PortModel> getVisibleInputsByDisplayOrder() {
        return getInputsByDisplayOrder();
    }

    public List<PortModel> getVisibleOutputsByDisplayOrder() {
        return getOutputsByDisplayOrder();
    }

    public Collection<PortModel> getInputPorts() {
        return getInputsById().values();
    }

    public Collection<PortModel> getOutputPorts() {
        return getOutputsById().values();
    }

    // ----------------------------
    // Node options
    // ----------------------------
    /**
     * Gets the node options defined on this node.
     *
     * @return the list of node options
     */
    public List<NodeOption> getNodeOptions() {
        return nodeOptions;
    }

    /**
     * Gets a node option by its unique name.
     *
     * @param name the unique name
     * @return the node option, or {@code null} if not found
     */
    public INodeOption getNodeOptionById(String name) {
        return nodeOptionsById.get(name);
    }

    /**
     * Clears all node options from this node.
     */
    public void clearNodeOptions() {
        nodeOptions.clear();
        nodeOptionsById.clear();
    }

    @Override
    public PortModel getPortFitToConnectTo(PortModel portModel) {
        if (graphModel == null) return null;
        // Find a compatible port to connect to
        var requiredDirection = portModel.getDirection().getOpposite();
        var candidates = requiredDirection == PortDirection.INPUT ? getInputsByDisplayOrder() : getOutputsByDisplayOrder();
        var compatiblePorts = graphModel.getCompatiblePorts(candidates, portModel);
        return !compatiblePorts.isEmpty() ? compatiblePorts.getFirst() : null;
    }
}
