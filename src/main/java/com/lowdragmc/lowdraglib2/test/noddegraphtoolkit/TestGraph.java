package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphLogger;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphNodeRegistry;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public class TestGraph extends Graph {
    public static final GraphNodeRegistry NODE_REGISTRY =
            GraphNodeRegistry.create(LDLib2.id("test_graph"), TestGraph.class);

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return NODE_REGISTRY.getNodeClasses();
    }

    // Cross-type subgraph support: a TestGraph may embed / import AnnotatedOtherGraph instances as
    // subgraphs (but not, e.g., ModFilteredTestGraph). Exercised by GraphCrossTypeSubgraphTest.
    @Override
    public boolean acceptsSubgraphGraph(Graph other) {
        return other instanceof AnnotatedOtherGraph;
    }

    @Override
    public @Nullable List<TypeHandle> getSupportTypes() {
        var supportTypes = new HashSet<>(CustomGraphModelImpl.detectSupportedTypes(graphModel));
        supportTypes.add(TypeHandles.DOUBLE);
        supportTypes.add(TypeHandles.INT);
        supportTypes.add(TypeHandles.STRING);
        supportTypes.add(TypeHandles.BOOL);
        supportTypes.add(TypeHandles.FLOAT);
        supportTypes.add(TypeHandles.LONG);
        supportTypes.add(TypeHandles.DIRECTION);

        return List.copyOf(supportTypes);
    }

    private int changeCount = 0;

    @Override
    public void onGraphChanged(GraphLogger logger) {
        changeCount++;
        logger.info(Component.literal("Graph changed: " + changeCount));
        logger.error(Component.literal("Graph changed: " + changeCount));
        logger.warning(Component.literal("Graph changed: " + changeCount));
    }
}
