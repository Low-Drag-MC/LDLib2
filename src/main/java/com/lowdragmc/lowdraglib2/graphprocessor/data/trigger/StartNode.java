package com.lowdragmc.lowdraglib2.graphprocessor.data.trigger;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "trigger start", group = "graph_processor.node.logic", registry = "ldlib2:graph_node")
public class StartNode extends BaseNode implements ITriggerableNode {
    @OutputPort(name = "Triggered", priority = -10000)
    public TriggerLink triggered;
}
