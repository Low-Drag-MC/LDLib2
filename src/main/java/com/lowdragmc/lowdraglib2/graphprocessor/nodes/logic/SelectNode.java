package com.lowdragmc.lowdraglib2.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "select", group = "graph_processor.node.logic", registry = "ldlib2:graph_node")
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
