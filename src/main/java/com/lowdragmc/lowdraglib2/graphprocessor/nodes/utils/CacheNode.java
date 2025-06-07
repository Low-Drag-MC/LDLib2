package com.lowdragmc.lowdraglib2.graphprocessor.nodes.utils;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.CustomPortOutput;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib2.graphprocessor.data.PortEdge;
import com.lowdragmc.lowdraglib2.graphprocessor.data.trigger.LinearTriggerNode;

import java.util.List;

@LDLRegister(name = "cache", group = "graph_processor.node.utils", registry = "ldlib2:graph_node")
public class CacheNode extends LinearTriggerNode {
    @InputPort
    public Object in;
    @OutputPort
    public Object out;

    @Override
    protected void process() {
        out = in;
    }

    @CustomPortBehavior(field = "out")
    public List<PortData> modifyOutPort(List<PortEdge> edges) {
        Class<?> type = Object.class;
        for (var inputPort : getInputPorts()) {
            if (inputPort.fieldName.equals("in")) {
                for (var edge : inputPort.getEdges()) {
                    if (edge.outputPort.portData.displayType != null) {
                        type = edge.outputPort.portData.displayType;
                        break;
                    } else {
                        type = edge.outputPort.fieldInfo.getType();
                        break;
                    }
                }
            }
        }
        return List.of(new PortData()
                .displayName("out")
                .identifier("out")
                .acceptMultipleEdges(true)
                .displayType(type));
    }

    @CustomPortOutput(field = "out")
    public void pushOut(List<PortEdge> outputEdges, NodePort outputPort) {
        for (var edge : outputEdges) {
            edge.passThroughBuffer = out;
        }
    }
}
