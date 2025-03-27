package com.lowdragmc.lowdraglib.graphprocessor.nodes;

import com.lowdragmc.lowdraglib.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.CustomPortInput;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortEdge;

import java.util.ArrayList;
import java.util.List;

public abstract class ListInputNode<I> extends BaseNode {
    @InputPort
    public List<I> inputs = new ArrayList<>();

    public abstract Class<I> type();

    @CustomPortBehavior(field = "inputs")
    public List<PortData> inputPortBehavior(List<PortEdge> edges) {
        var ports = new ArrayList<PortData>();
        for (int i = 0; i < edges.size() + 1; i++) {
            var identifier = String.valueOf(i);
            if (i < edges.size()) {
                var edge = edges.get(i);
                edge.inputPortIdentifier = identifier;
            }
            ports.add(new PortData()
                    .displayName("in " + i)
                    .identifier(identifier)
                    .displayType(type()));
        }
        if (inputs == null) {
            inputs = new ArrayList<>();
        }
        inputs.clear();
        while (inputs.size() + 1 < ports.size()) {
            inputs.add(null);
        }
        return ports;
    }

    // This function will be called once per port created from the `inputs` custom port function
    // will in parameter the list of the edges connected to this port
    @CustomPortInput(field = "inputs")
    public void pullInputs(List<PortEdge> inputEdges, NodePort inputPort) {
        if (inputEdges.isEmpty()) return;
        I value = null;
        // we only find the first available edge
        for (PortEdge inputEdge : inputEdges) {
            if (inputEdge.passThroughBuffer != null && type().isAssignableFrom(inputEdge.passThroughBuffer.getClass())) {
                value = (I) inputEdge.passThroughBuffer;
                break;
            }
        }
        var index = inputPort.owner.getInputPorts().indexOf(inputPort);
        while (inputs.size() <= index) {
            inputs.add(value);
        }
        inputs.set(index, value);
    }

}
