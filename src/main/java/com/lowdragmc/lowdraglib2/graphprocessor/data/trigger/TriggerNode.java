package com.lowdragmc.lowdraglib2.graphprocessor.data.trigger;

import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;


public abstract class TriggerNode extends BaseNode implements ITriggerableNode {
    @InputPort(name = "Trigger", allowMultiple = true, priority = -10000)
    public TriggerLink trigger;
}
