package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.fluid;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@LDLRegister(name = "fluid transfer info", group = "graph_processor.node.minecraft.fluid", registry = "ldlib:graph_node")
public class FluidTransferInfoNode extends BaseNode {
    @InputPort(name = "fluid transfer")
    public IFluidHandler fluidTransfer;
    @InputPort(name = "tank index")
    public Integer tank;
    @OutputPort(name = "tank size")
    public int tanks;
    @OutputPort()
    public FluidStack fluidstack;
    @OutputPort(name = "capacity")
    public int capacity;
    @Configurable(name = "tank index")
    public int internalTank;

    @Override
    public void process() {
        if (fluidTransfer != null) {
            tanks = fluidTransfer.getTanks();
            var realTank = tank == null ? internalTank : tank;
            fluidstack = fluidTransfer.getFluidInTank(realTank);
            capacity = fluidTransfer.getTankCapacity(realTank);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("slot")) {
                if (!port.getEdges().isEmpty()) return;
            }
        }
        super.buildConfigurator(father);
    }
}
