package com.lowdragmc.lowdraglib.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "random", group = "graph_processor.node.math", registry = "ldlib:graph_node")
public class RandomNode extends BaseNode {
    @InputPort
    public float min = 0;
    @InputPort
    public float max = 0;
    @OutputPort
    public float out = 0;

    @Override
    public void process() {
        if (max == min) {
            out = min;
            return;
        }
        out = min + LDLib.RANDOM.nextFloat() * (max - min);
    }
}
