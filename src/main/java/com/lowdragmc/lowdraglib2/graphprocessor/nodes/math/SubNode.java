package com.lowdragmc.lowdraglib2.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "sub", group = "graph_processor.node.math", registry = "ldlib2:graph_node")
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
