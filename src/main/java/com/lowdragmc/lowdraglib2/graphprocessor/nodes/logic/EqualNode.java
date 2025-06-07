package com.lowdragmc.lowdraglib2.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

import java.util.Objects;

@LDLRegister(name = "equal", group = "graph_processor.node.logic", registry = "ldlib2:graph_node")
public class EqualNode extends BaseNode {
    @InputPort
    public Object a;
    @InputPort
    public Object b;
    @OutputPort
    public boolean out;

    @Override
    public void process() {
        out = Objects.equals(a, b);
    }
}
