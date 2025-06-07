package com.lowdragmc.lowdraglib2.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "xor", group = "graph_processor.node.logic", registry = "ldlib2:graph_node")
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
