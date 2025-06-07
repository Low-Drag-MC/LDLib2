package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.fluid;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.utils.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.trigger.LinearTriggerNode;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.lang.reflect.Method;
import java.util.HashMap;

@LDLRegister(name = "fluid fill", group = "graph_processor.node.minecraft.fluid", registry = "ldlib2:graph_node")
public class FluidTransferFillNode extends LinearTriggerNode {
    @InputPort(name = "fluid transfer")
    public IFluidHandler fluidTransfer;
    @InputPort
    public FluidStack fluidstack;
    @InputPort
    public Boolean simulate;
    @OutputPort
    public int filled;
    @Configurable(name = "simulate")
    public boolean internalSimulate;

    @Override
    public void process() {
        filled = 0;
        if (fluidTransfer != null && fluidstack != null) {
            filled = (int) fluidTransfer.fill(fluidstack, simulate == null ? internalSimulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE : simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        var setter = new HashMap<String, Method>();
        var clazz = getClass();
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("simulate")) {
                if (port.getEdges().isEmpty()) {
                    try {
                        ConfiguratorParser.createFieldConfigurator(clazz.getField("internalSimulate"), father, clazz, setter, this);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
