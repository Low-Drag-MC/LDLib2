package com.lowdragmc.lowdraglib.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "xor", group = "graph_processor.node.logic", registry = "ldlib:graph_node")
public class XorNode extends BaseNode {
    @InputPort
    public boolean a = false;
    @InputPort
    public boolean b = false;
    @OutputPort
    public boolean out = false;

    @Override
    public void process() {
        out = a ^ b;
    }
}
