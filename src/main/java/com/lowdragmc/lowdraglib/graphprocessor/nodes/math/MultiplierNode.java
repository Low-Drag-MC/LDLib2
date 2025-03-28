package com.lowdragmc.lowdraglib.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.nodes.ListMergeNode;

@LDLRegister(name = "multiplier", group = "graph_processor.node.math", registry = "ldlib:graph_node")
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
