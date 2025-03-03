package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;

@LDLRegister(name = "select", group = "graph_processor.node.logic")
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
