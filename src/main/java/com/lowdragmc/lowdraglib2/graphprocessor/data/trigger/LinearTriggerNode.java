package com.lowdragmc.lowdraglib2.graphprocessor.data.trigger;

import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;

public class LinearTriggerNode extends TriggerNode {
    @OutputPort(name = "Triggered", priority = -10000)
    public TriggerLink triggered;
}
