package com.lowdragmc.lowdraglib.graphprocessor.data.trigger;

import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;

public class LinearTriggerNode extends TriggerNode {
    @OutputPort(name = "Triggered", priority = -10000)
    public TriggerLink triggered;
}
