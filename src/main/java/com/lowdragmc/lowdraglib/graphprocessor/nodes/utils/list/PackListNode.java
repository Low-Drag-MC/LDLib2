package com.lowdragmc.lowdraglib.graphprocessor.nodes.utils.list;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.nodes.ListInputNode;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "pack list", group = "graph_processor.node.utils.list")
public class PackListNode extends ListInputNode<Object> {
    @OutputPort
    public List<Object> out = new ArrayList<>();

    @Override
    public Class<Object> type() {
        return Object.class;
    }

    @Override
    protected void process() {
        out.clear();
        if (inputs != null) {
            out.addAll(inputs);
        }
    }

}
