package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;

import java.util.List;

public class TestGraph extends Graph {
    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return List.of(
                TestAddNode.class,
                TestConstantNode.class,
                TestStringConcatNode.class
        );
    }
}
