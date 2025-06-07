package com.lowdragmc.lowdraglib2.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "random", group = "graph_processor.node.math", registry = "ldlib2:graph_node")
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
        out = min + LDLib2.RANDOM.nextFloat() * (max - min);
    }
}
