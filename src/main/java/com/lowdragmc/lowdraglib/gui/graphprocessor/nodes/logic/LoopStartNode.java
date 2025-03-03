package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.TriggerNode;

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
