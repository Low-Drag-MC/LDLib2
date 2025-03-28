package com.lowdragmc.lowdraglib.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "sub", group = "graph_processor.node.math", registry = "ldlib:graph_node")
public class SubNode extends BaseNode {
    @InputPort
    public float a = 0;
    @InputPort
    public float b = 0;
    @OutputPort
    public float out = 0;

    @Override
    public void process() {
        out = a - b;
    }
}
