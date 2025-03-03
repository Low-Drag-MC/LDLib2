package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.utils.list;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.CustomPortOutput;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.*;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "list reader", group = "graph_processor.node.utils.list")
public class ListReaderNode extends BaseNode {
    @InputPort
    public List<Object> in = new ArrayList<>();
    @InputPort
    public int index;
    @OutputPort
    public int size;
    @OutputPort
    public Object out;

    @Override
    protected void process() {
        if (in == null || in.isEmpty()) {
            out = null;
            size = 0;
            return;
        } else if (index < 0 || index >= in.size()) {
            out = null;
        } else {
            out = in.get(index);
        }
        size = in.size();
    }

    @CustomPortBehavior(field = "out")
    public List<PortData> modifyIfPort(List<PortEdge> edges) {
        return List.of(new PortData()
                .displayName("out")
                .identifier("out")
                .acceptMultipleEdges(true)
                .displayType(Object.class));
    }

    @CustomPortOutput(field = "out")
    public void pushOut(List<PortEdge> outputEdges, NodePort outputPort) {
        for (var edge : outputEdges) {
            edge.passThroughBuffer = out;
        }
    }

}
