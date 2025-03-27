package com.lowdragmc.lowdraglib.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.TriggerLink;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.TriggerNode;

import java.util.List;

@LDLRegister(name = "if else", group = "graph_processor.node.logic")
public class IfElseNode extends TriggerNode {
    @InputPort
    public boolean condition;
    @OutputPort(name = "True", priority = -10001)
    public TriggerLink _true;
    @OutputPort(name = "Else", priority = -10000)
    public TriggerLink _false;

    @Override
    public List<TriggerNode> getNextTriggerNodes() {
        var fieldName = condition ? "_true" : "_false";
        return outputPorts.stream()
            .filter(p -> p.fieldName.equals(fieldName))
            .findFirst()
            .map(p -> p.getEdges().stream().map(e -> (TriggerNode) e.inputNode).toList())
            .orElse(List.of());
    }
}
