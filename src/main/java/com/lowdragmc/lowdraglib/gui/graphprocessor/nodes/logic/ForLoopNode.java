package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.TriggerLink;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.TriggerNode;

import java.util.Collections;
import java.util.List;

@LDLRegister(name = "for loop", group = "graph_processor.node.logic")
public class ForLoopNode extends TriggerNode {
    @OutputPort(name = "loop body")
    public TriggerLink loopBody;
    @OutputPort(name = "loop completed")
    public TriggerLink loopCompleted;
    @OutputPort
    public int index;
    @InputPort
    public int start;
    @InputPort
    public int end;
    public boolean isLooping = false;

    @Override
    protected void process() {
        index++;
    }

    @Override
    public void resetNode() {
        super.resetNode();
        isLooping = false;
    }

    @Override
    public List<TriggerNode> getNextTriggerNodes() {
        for (var port : self().getOutputPorts()) {
            if (port.fieldInfo.getType() == TriggerLink.class && port.fieldInfo.getName().equals("loopCompleted")) {
                return port.getEdges().stream().map(e -> (TriggerNode)e.inputNode).toList();
            }
        }
        return Collections.emptyList();
    }


    public List<TriggerNode> getExecutedNodesLoopBody() {
        for (var port : self().getOutputPorts()) {
            if (port.fieldInfo.getType() == TriggerLink.class && port.fieldInfo.getName().equals("loopBody")) {
                return port.getEdges().stream().map(e -> (TriggerNode)e.inputNode).toList();
            }
        }
        return Collections.emptyList();
    }
}
