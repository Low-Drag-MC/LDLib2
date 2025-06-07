package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

@LDLRegister(name = "item transfer info", group = "graph_processor.node.minecraft.item", registry = "ldlib2:graph_node")
public class ItemTransferInfoNode extends BaseNode {
    @InputPort(name = "item transfer")
    public IItemHandler itemTransfer;
    @InputPort(name = "slot index")
    public Integer slot;
    @OutputPort(name = "slot size")
    public int slots;
    @OutputPort
    public ItemStack itemstack;
    @OutputPort(name = "slot limit")
    public int slotLimit;
    @Configurable(name = "slot index")
    public int internalSlot;

    @Override
    public void process() {
        if (itemTransfer != null) {
            slots = itemTransfer.getSlots();
            var realSlot = slot == null ? internalSlot : slot;
            itemstack = itemTransfer.getStackInSlot(realSlot);
            slotLimit = itemTransfer.getSlotLimit(realSlot);
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
