package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.data;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.nbt.CompoundTag;

@LDLRegister(name = "compound merge", group = "graph_processor.node.minecraft.data")
public class CompoundMergeNode extends LinearTriggerNode {

    @InputPort
    public CompoundTag a;
    @InputPort
    public CompoundTag b;

    @OutputPort
    public CompoundTag out;

    @Override
    public void process() {
        if (a != null && b != null) {
            out = a.copy();
            out.merge(b);
        } else if (a != null) {
            out = a.copy();
        } else if (b != null) {
            out = b.copy();
        } else {
            out = null;
        }
    }

}
