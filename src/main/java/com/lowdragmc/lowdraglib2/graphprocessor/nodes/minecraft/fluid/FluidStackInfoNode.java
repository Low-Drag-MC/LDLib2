package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.fluid;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.material.Fluid;

@LDLRegister(name = "fluidstack info", group = "graph_processor.node.minecraft.fluid", registry = "ldlib2:graph_node")
public class FluidStackInfoNode extends BaseNode {
    @InputPort
    public FluidStack in = null;
    @OutputPort
    public Fluid out = null;
    @OutputPort
    public int amount = 0;
    @OutputPort
    public DataComponentMap components;

    @Override
    public void process() {
        out = null;
        amount = 0;
        components = null;
        if (in != null) {
            out = in.getFluid();
            amount = (int) in.getAmount();
            components = in.getComponents();
        }
    }

}
