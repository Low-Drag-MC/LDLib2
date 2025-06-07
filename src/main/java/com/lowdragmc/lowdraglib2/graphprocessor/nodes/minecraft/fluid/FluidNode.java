package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.fluid;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

@LDLRegister(name = "fluid", group = "graph_processor.node.minecraft.fluid", registry = "ldlib2:graph_node")
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
            if (LDLib2.isValidResourceLocation(name)) {
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
