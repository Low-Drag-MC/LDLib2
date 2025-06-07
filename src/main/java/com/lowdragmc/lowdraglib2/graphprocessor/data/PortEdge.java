package com.lowdragmc.lowdraglib2.graphprocessor.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

public final class PortEdge implements IPersistedSerializable {
    @Persisted
    public String GUID;
    @Persisted
    public String inputNodeGUID;
    @Persisted
    public String outputNodeGUID;
    @Persisted
    public String inputFieldName;
    @Persisted
    public String outputFieldName;
    // Use to store the id of the field that generate multiple ports
    @Persisted
    public String inputPortIdentifier;
    @Persisted
    public String outputPortIdentifier;

    // runtime
    private BaseGraph owner;
    public BaseNode	inputNode;
    public NodePort	inputPort;
    public NodePort outputPort;

    //temporary object used to send port to port data when a custom input/output function is used.
    public Object passThroughBuffer;

    public BaseNode	outputNode;

    public PortEdge() {}

    public static PortEdge createNewEdge(BaseGraph graph, NodePort inputPort, NodePort outputPort) {
        var	edge = new PortEdge();
        edge.owner = graph;
        edge.GUID = graph.newGUID().toString();
        edge.inputNode = inputPort.owner;
        edge.inputFieldName = inputPort.fieldName;
        edge.outputNode = outputPort.owner;
        edge.outputFieldName = outputPort.fieldName;
        edge.inputPort = inputPort;
        edge.outputPort = outputPort;
        edge.inputPortIdentifier = inputPort.portData.identifier;
        edge.outputPortIdentifier = outputPort.portData.identifier;
        return edge;
    }

    public void onBeforeSerialize() {
        if (outputNode == null || inputNode == null)
            return;
        outputNodeGUID = outputNode.getGUID();
        inputNodeGUID = inputNode.getGUID();
    }


    @Override
    public void beforeSerialize() {
        onBeforeSerialize();
    }

    public PortEdge copy() {
        var edge = new PortEdge();
        edge.deserializeNBT(Platform.getFrozenRegistry(), serializeNBT(Platform.getFrozenRegistry()));
        edge.GUID = null;
        edge.inputNodeGUID = null;
        edge.outputNodeGUID = null;
        return edge;
    }

    public void initialize(BaseGraph owner) {
        this.owner = owner;
        if (owner == null || inputNodeGUID == null || outputNodeGUID == null)
            return;
        if (!owner.nodesPerGUID.containsKey(outputNodeGUID) || !owner.nodesPerGUID.containsKey(inputNodeGUID))
            return ;

        outputNode = owner.nodesPerGUID.get(outputNodeGUID);
        inputNode = owner.nodesPerGUID.get(inputNodeGUID);
        inputPort = inputNode.getPort(inputFieldName, inputPortIdentifier);
        outputPort = outputNode.getPort(outputFieldName, outputPortIdentifier);
    }

    @Override
    public String toString(){
        return "%s:%s -> %s:%s".formatted(outputNode.name(), outputPort.fieldName, inputNode.name(), inputPort.fieldName);
    };
}
