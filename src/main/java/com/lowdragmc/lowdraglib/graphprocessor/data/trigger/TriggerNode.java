package com.lowdragmc.lowdraglib.graphprocessor.data.trigger;

import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;


public abstract class TriggerNode extends BaseNode implements ITriggerableNode {
    @InputPort(name = "Trigger", allowMultiple = true, priority = -10000)
    public TriggerLink trigger;
}
