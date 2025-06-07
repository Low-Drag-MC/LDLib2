package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.fluid;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

@LDLRegister(name = "fluidstack", group = "graph_processor.node.minecraft.fluid", registry = "ldlib2:graph_node")
public class FluidStackNode extends BaseNode {
    @InputPort
    public Object in;
    @InputPort
    public Fluid fluid;
    @InputPort
    public Integer amount;
    @InputPort
    public DataComponentPatch components;
    @OutputPort
    public FluidStack out;
    @Configurable(name = "fluidstack", canCollapse = false, collapse = false)
    public FluidStack internalValue = new FluidStack(Fluids.WATER, 1000);

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue.copy();
        } else if (in instanceof FluidStack fluidStack){
            out = fluidStack.copy();
        } else if (in instanceof CompoundTag fluidTag) {
            out = FluidStack.OPTIONAL_CODEC.parse(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), fluidTag).getOrThrow();
        } else {
            out = FluidStack.EMPTY;
        }
        if (fluid != null) {
            var stack = new FluidStack(fluid, out.getAmount());
            if (components != null) {
                out.getComponents().applyPatch(components);
            }
            out = stack;
        }
        if (amount != null) {
            out.setAmount(amount);
        }
        if (components != null) {
            out.getComponents().applyPatch(components);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("in")) {
                if (!port.getEdges().isEmpty()) return;
            }
        }
        super.buildConfigurator(father);
    }
}
