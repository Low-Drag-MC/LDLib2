package com.lowdragmc.lowdraglib2.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.nodes.ListMergeNode;

@LDLRegister(name = "multiplier", group = "graph_processor.node.math", registry = "ldlib2:graph_node")
public class MultiplierNode extends ListMergeNode<Float> {

    @Override
    public Class<Float> type() {
        return Float.class;
    }

    @Override
    public Float defaultValue() {
        return 1f;
    }

    @Override
    public Float merge(Float a, Float b) {
        return a * b;
    }
}
