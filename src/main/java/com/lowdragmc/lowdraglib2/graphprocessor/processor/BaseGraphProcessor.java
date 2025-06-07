package com.lowdragmc.lowdraglib2.graphprocessor.processor;

import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;

import javax.annotation.Nonnull;
import java.util.Iterator;

public abstract class BaseGraphProcessor implements Iterable<BaseNode> {
    protected BaseGraph graph;

    /**
     * Manage graph scheduling and processing
     */
    public BaseGraphProcessor(BaseGraph graph) {
        this.graph = graph;
    }

    /**
     * Call it when the graph order updated.
     * <br>
     * please call it before {@link BaseGraphProcessor#run()}.
     */
    public abstract void updateComputeOrder();

    /**
     * Run the entire graph
     */
    public void run() {
        var iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next();
        }
    }

    /**
     * Run the next step of the graph
     * @return iterator of next processed nodes
     */
    @Nonnull
    public abstract Iterator<BaseNode> iterator();
}
