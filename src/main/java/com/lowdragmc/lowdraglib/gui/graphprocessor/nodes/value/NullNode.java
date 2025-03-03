package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.value;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;

@LDLRegister(name = "null", group = "graph_processor.node.value")
public class NullNode extends BaseNode {

    @OutputPort
    public Object out;

    @Override
    public void process() {
        out = null;
    }

}
