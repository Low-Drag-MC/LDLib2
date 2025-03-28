package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.fluid;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.utils.ConfiguratorParser;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.LinearTriggerNode;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.lang.reflect.Method;
import java.util.HashMap;

@LDLRegister(name = "fluid drain", group = "graph_processor.node.minecraft.fluid", registry = "ldlib:graph_node")
public class FluidTransferDrainNode extends LinearTriggerNode {
    @InputPort(name = "fluid transfer")
    public IFluidHandler fluidTransfer;
    @InputPort
    public FluidStack fluidstack;
    @InputPort
    public Boolean simulate;
    @OutputPort
    public FluidStack drained;
    @Configurable(name = "simulate")
    public boolean internalSimulate;

    @Override
    public void process() {
        drained = null;
        if (fluidTransfer != null && fluidstack != null) {
            drained = fluidTransfer.drain(fluidstack, simulate == null ? internalSimulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE : simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
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
