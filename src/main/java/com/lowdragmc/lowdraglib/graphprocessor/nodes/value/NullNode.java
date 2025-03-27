package com.lowdragmc.lowdraglib.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;

@LDLRegister(name = "null", group = "graph_processor.node.value")
public class NullNode extends BaseNode {

    @OutputPort
    public Object out;

    @Override
    public void process() {
        out = null;
    }

}
