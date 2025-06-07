package com.lowdragmc.lowdraglib2.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

@LDLRegister(name = "null", group = "graph_processor.node.value", registry = "ldlib2:graph_node")
public class NullNode extends BaseNode {

    @OutputPort
    public Object out;

    @Override
    public void process() {
        out = null;
    }

}
