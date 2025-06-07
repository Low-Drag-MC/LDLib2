package com.lowdragmc.lowdraglib2.graphprocessor.data.trigger;

import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface ITriggerableNode {

    default BaseNode self() {
        return (BaseNode) this;
    }

    /**
     * Get all nodes are waiting to be triggered. after this node is executed.
     */
    default List<TriggerNode> getNextTriggerNodes() {
        for (var port : self().getOutputPorts()) {
            if (port.fieldInfo.getType() == TriggerLink.class) {
                return port.getEdges().stream().map(e -> (TriggerNode)e.inputNode).toList();
            }
        }
        return Collections.emptyList();
    }

    /**
     * The node is triggered.
     * @param triggerSource trigger source
     */
    default void onTrigger(@Nullable ITriggerableNode triggerSource) {
        self().onProcess();
    }
}
