package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes;

import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.CustomPortInput;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortEdge;
import com.lowdragmc.lowdraglib.utils.TypeAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class ListMergeNode<T> extends BaseNode {
    @InputPort
    public List<T> inputs = new ArrayList<>();
    @OutputPort
    public T out;

    public abstract Class<T> type();
    public abstract T defaultValue();
    public abstract T merge(T a, T b);

    @Override
    public void process() {
        out = defaultValue();
        if (inputs != null) {
            out = inputs.stream().reduce(out, this::merge);
        }
    }

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
            inputs.add(defaultValue());
        }
        return ports;
    }

    @CustomPortBehavior(field = "out")
    public List<PortData> outputPortBehavior(List<PortEdge> edges) {
        return List.of(new PortData()
                .displayName("out")
                .identifier("out")
                .acceptMultipleEdges(true)
                .displayType(type()));
    }

    // This function will be called once per port created from the `inputs` custom port function
    // will in parameter the list of the edges connected to this port
    @CustomPortInput(field = "inputs")
    public void pullInputs(List<PortEdge> inputEdges, NodePort inputPort) {
        if (inputEdges.isEmpty()) return;
        var value = defaultValue();
        for (PortEdge inputEdge : inputEdges) {
            var edgeType = inputEdge.passThroughBuffer.getClass();
            var type = type();
            if (edgeType == type) {
                value = merge(value, (T) inputEdge.passThroughBuffer);
            } else if (TypeAdapter.areConvertable(edgeType, type)) {
                value = merge(value, (T) TypeAdapter.convert(inputEdge.passThroughBuffer, type()));
            }
        }
        var index = inputPort.owner.getInputPorts().indexOf(inputPort);
        while (inputs.size() <= index) {
            inputs.add(value);
        }
        inputs.set(index, value);
    }

    @CustomPortInput(field = "out")
    public void pushOutputs(List<PortEdge> outputEdges, NodePort inputPort) {
        for (var edge : outputEdges) {
            edge.passThroughBuffer = out;
        }
    }
}
