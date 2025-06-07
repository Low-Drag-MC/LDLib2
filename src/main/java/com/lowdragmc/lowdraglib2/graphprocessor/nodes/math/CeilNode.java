package com.lowdragmc.lowdraglib2.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "ceil", group = "graph_processor.node.math", registry = "ldlib2:graph_node")
public class CeilNode extends BaseNode {
    @InputPort
    public float in = 0;
    @OutputPort
    public float out = 0;

    @Override
    public void process() {
        out = (float) Math.ceil(in);
    }
}
