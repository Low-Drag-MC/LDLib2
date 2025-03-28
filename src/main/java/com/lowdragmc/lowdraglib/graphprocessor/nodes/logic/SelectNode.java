package com.lowdragmc.lowdraglib.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "select", group = "graph_processor.node.logic", registry = "ldlib:graph_node")
public class SelectNode extends BaseNode {
    @InputPort(name = "true")
    public Object _true;
    @InputPort(name = "false")
    public Object _false;
    @InputPort
    public boolean condition;
    @OutputPort
    public Object out;

    @Override
    public void process() {
        out = condition ? _true : _false;
    }
}
