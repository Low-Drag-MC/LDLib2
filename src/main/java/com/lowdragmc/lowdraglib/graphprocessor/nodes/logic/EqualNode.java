package com.lowdragmc.lowdraglib.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

import java.util.Objects;

@LDLRegister(name = "equal", group = "graph_processor.node.logic")
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
