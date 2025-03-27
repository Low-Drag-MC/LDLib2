package com.lowdragmc.lowdraglib.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.nodes.ListMergeNode;

@LDLRegister(name = "max", group = "graph_processor.node.math")
public class MaxNode extends ListMergeNode<Float> {
    @Override
    public Class<Float> type() {
        return Float.class;
    }

    @Override
    public Float defaultValue() {
        return -Float.MIN_VALUE;
    }

    @Override
    public Float merge(Float a, Float b) {
        return Math.max(a, b);
    }
}
