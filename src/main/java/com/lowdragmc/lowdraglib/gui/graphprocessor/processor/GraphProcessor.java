package com.lowdragmc.lowdraglib.gui.graphprocessor.processor;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * The common implementation which won't take care of the Triggerable nodes.
 * Triggerable nodes will be regarded as base node as well.
 */
public class GraphProcessor extends BaseGraphProcessor {
    protected List<BaseNode> processList = Collections.emptyList();

    public GraphProcessor(BaseGraph graph) {
        super(graph);
    }

    @Override
    public void updateComputeOrder() {
        processList = graph.nodes.stream().sorted(Comparator.comparing(BaseNode::getComputeOrder)).toList();
    }

    @Override
    public @NotNull Iterator<BaseNode> iterator() {
        graph.resetNodes();
        var iterator = processList.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public BaseNode next() {
                var node = iterator.next();
                node.onProcess();
                return node;
            }
        };
    }
}
