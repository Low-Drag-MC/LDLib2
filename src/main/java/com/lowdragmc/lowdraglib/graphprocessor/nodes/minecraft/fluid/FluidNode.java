package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.fluid;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

@LDLRegister(name = "fluid", group = "graph_processor.node.minecraft.fluid", registry = "ldlib:graph_node")
public class FluidNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public Fluid out = null;
    @Configurable(showName = false)
    public Fluid internalValue = Fluids.WATER;

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof Fluid fluid) {
            out = fluid;
        } else if (in instanceof FluidStack fluidStack) {
            out = fluidStack.getFluid();
        } else {
            var name = in.toString();
            if (LDLib.isValidResourceLocation(name)) {
                out = BuiltInRegistries.FLUID.get(ResourceLocation.parse(name));
            } else {
                out = null;
            }
        }
        internalValue = out;
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
