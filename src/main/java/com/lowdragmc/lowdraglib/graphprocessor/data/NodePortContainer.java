package com.lowdragmc.lowdraglib.graphprocessor.data;

import com.lowdragmc.lowdraglib.LDLib;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Container of ports and the edges connected to these ports
 */
public abstract class NodePortContainer extends ArrayList<NodePort> {
    protected BaseNode node;

    public NodePortContainer(BaseNode node) {
        this.node = node;
    }

    /**
     * Remove an edge that is connected to one of the node in the container
     */
    public void remove(PortEdge edge) {
        forEach(p -> p.remove(edge));
    }

    /**
     * Add an edge that is connected to one of the node in the container
     */
    public void add(PortEdge edge) {
        var portFieldName = (edge.inputNode == node) ? edge.inputFieldName : edge.outputFieldName;
        var portIdentifier = (edge.inputNode == node) ? edge.inputPortIdentifier : edge.outputPortIdentifier;

        // Force empty string to null since portIdentifier is a serialized value
        var identifier = (portIdentifier == null || portIdentifier.isEmpty()) ? null : portIdentifier;
        var port = this.stream().filter(p ->
                Objects.equals(p.fieldName, portFieldName) &&
                Objects.equals(p.portData.identifier, identifier))
                .findFirst();
        if (port.isEmpty()) {
            LDLib.LOGGER.error("The edge can't be properly connected because it's ports can't be found");
            return;
        }

        port.get().add(edge);
    }

    public static class NodeInputPortContainer extends NodePortContainer {
		public NodeInputPortContainer(BaseNode node) {
            super(node);
        }

        public void PullDatas() {
            forEach(NodePort::PullData);
        }
    }

    public static class NodeOutputPortContainer extends NodePortContainer {
		public NodeOutputPortContainer(BaseNode node) {
            super(node);
        }

        public void PushDatas() {
            forEach(NodePort::PushData);
        }
    }
}
