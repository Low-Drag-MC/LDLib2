package com.lowdragmc.lowdraglib2.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "mod", group = "graph_processor.node.math", registry = "ldlib2:graph_node")
public class ModNode extends BaseNode {
    @InputPort
    public int a = 0;
    @InputPort
    public int b = 0;
    @OutputPort
    public int out = 0;

    @Override
    public void process() {
        out = Math.floorMod(a, b);
    }
}
