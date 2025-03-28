package com.lowdragmc.lowdraglib.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.*;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortEdge;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "switch", group = "graph_processor.node.logic", registry = "ldlib:graph_node")
public class SwitchNode extends BaseNode {
    @InputPort
    public int index = 0;
    @InputPort
    public List inputs = new ArrayList<>();
    @OutputPort
    public Object out;

    // runtime
    protected Class<?> listType = Object.class;

    @Override
    public void process() {
        out = null;
        if (inputs != null && index < inputs.size() && index >= 0) {
            out = inputs.get(index);
        }
    }

    @CustomPortBehavior(field = "inputs")
    public List<PortData> listPortBehavior(List<PortEdge> edges) {
        var ports = new ArrayList<PortData>();
        if (!edges.isEmpty()) {
            // if list is not empty, get the type of the first element
            for (var edge : edges) {
                if (edge.outputPort.portData.displayType != null) {
                    listType = edge.outputPort.portData.displayType;
                    break;
                }
            }
        } else {
            listType = Object.class;
        }
        for (int i = 0; i < edges.size() + 1; i++) {
            var identifier = String.valueOf(i);
            if (i < edges.size()) {
                var edge = edges.get(i);
                edge.inputPortIdentifier = identifier;
                var existingPort = edges.get(i).inputPort.portData;
                if (identifier.equals(existingPort.identifier)) {
                    existingPort.displayType = listType; // make sure we have the same type
                    ports.add(existingPort);
                    continue;
                }
            }
            ports.add(new PortData()
                    .displayName("in " + i)
                    .identifier(String.valueOf(i))
                    .displayType(listType));
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

    @CustomPortBehavior(field = "out")
    public List<PortData> modifyOutPort(List<PortEdge> edges) {
        return List.of(new PortData()
                .displayName("out")
                .identifier("out")
                .acceptMultipleEdges(true)
                .displayType(listType));
    }

    @CustomPortInput(field = "inputs")
    public void pullInputs(List<PortEdge> inputEdges, NodePort inputPort) {
        if (inputEdges.isEmpty()) {
            return;
        }
        Object value = null;
        for (PortEdge inputEdge : inputEdges) {
            value = inputEdge.passThroughBuffer;
        }
        var index = inputPort.owner.getInputPorts().indexOf(inputPort) - 1;
        while (inputs.size() <= index) {
            inputs.add(value);
        }
        inputs.set(index, value);
    }

    @CustomPortOutput(field = "out")
    public void pushOut(List<PortEdge> outputEdges, NodePort outputPort) {
        for (var edge : outputEdges) {
            edge.passThroughBuffer = out;
        }
    }

}
