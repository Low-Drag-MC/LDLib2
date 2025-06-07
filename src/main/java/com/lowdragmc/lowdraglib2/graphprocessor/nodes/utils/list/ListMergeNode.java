package com.lowdragmc.lowdraglib2.graphprocessor.nodes.utils.list;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@LDLRegister(name = "list merge", group = "graph_processor.node.utils.list", registry = "ldlib2:graph_node")
public class ListMergeNode extends BaseNode {
    @InputPort
    public List<Object> a = new ArrayList<>();
    @InputPort
    public List<Object> b = new ArrayList<>();
    @OutputPort
    public List<Object> out = new ArrayList<>();

    @Override
    protected void process() {
        var A = a == null || a.isEmpty() ? Collections.emptyList() : a;
        var B = b == null || b.isEmpty() ? Collections.emptyList() : b;
        if (out == null) out = new ArrayList<>(A.size() + B.size());
        out.clear();
        out.addAll(A);
        out.addAll(B);
    }

}
