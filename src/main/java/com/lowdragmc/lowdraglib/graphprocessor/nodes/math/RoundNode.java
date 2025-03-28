package com.lowdragmc.lowdraglib.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "round", group = "graph_processor.node.math", registry = "ldlib:graph_node")
public class RoundNode extends BaseNode {
    @InputPort
    public float in = 0;
    @OutputPort
    public int out = 0;

    @Override
    public void process() {
        out = Math.round(in);
    }
}
