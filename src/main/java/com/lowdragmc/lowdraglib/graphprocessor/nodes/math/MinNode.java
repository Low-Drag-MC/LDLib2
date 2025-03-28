package com.lowdragmc.lowdraglib.graphprocessor.nodes.math;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.nodes.ListMergeNode;

@LDLRegister(name = "min", group = "graph_processor.node.math", registry = "ldlib:graph_node")
public class MinNode extends ListMergeNode<Float> {

    @Override
    public Class<Float> type() {
        return Float.class;
    }

    @Override
    public Float defaultValue() {
        return Float.MAX_VALUE;
    }

    @Override
    public Float merge(Float a, Float b) {
        return Math.min(a, b);
    }
}
