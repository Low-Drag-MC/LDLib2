package com.lowdragmc.lowdraglib.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "floor", group = "graph_processor.node.math")
public class FloorNode extends BaseNode {
    @InputPort
    public float in = 0;
    @OutputPort
    public float out = 0;

    @Override
    public void process() {
        out = (float) Math.floor(in);
    }
}
