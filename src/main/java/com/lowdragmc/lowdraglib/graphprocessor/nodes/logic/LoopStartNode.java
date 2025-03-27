package com.lowdragmc.lowdraglib.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.TriggerNode;

public class LoopStartNode extends TriggerNode {
    @Override
    public String name() {
        return "loop start";
    }

    @Override
    public String group() {
        return "graph_processor.node.logic";
    }
}
