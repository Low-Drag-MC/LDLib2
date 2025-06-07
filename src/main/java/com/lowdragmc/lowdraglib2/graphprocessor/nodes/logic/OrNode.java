package com.lowdragmc.lowdraglib2.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.nodes.ListMergeNode;

@LDLRegister(name = "or", group = "graph_processor.node.logic", registry = "ldlib2:graph_node")
public class OrNode extends ListMergeNode<Boolean> {

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }

    @Override
    public Boolean defaultValue() {
        return false;
    }

    @Override
    public Boolean merge(Boolean a, Boolean b) {
        return a || b;
    }
}
