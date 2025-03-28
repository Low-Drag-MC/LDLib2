package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.utils.ConfiguratorParser;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.HashMap;

@LDLRegister(name = "item insert", group = "graph_processor.node.minecraft.item", registry = "ldlib:graph_node")
public class ItemTransferInsertNode extends LinearTriggerNode {
    @InputPort(name = "item transfer")
    public IItemHandler itemTransfer;
    @InputPort
    public ItemStack itemstack;
    @InputPort(name = "slot index")
    public Integer slot;
    @InputPort
    public Boolean simulate;
    @OutputPort
    public ItemStack remaining;
    @Configurable(name = "slot index")
    public int internalSlot;
    @Configurable(name = "simulate")
    public boolean internalSimulate;

    @Override
    public void process() {
        remaining = null;
        if (itemTransfer != null && itemstack != null) {
            remaining = itemTransfer.insertItem(
                    slot == null ? internalSlot : slot,
                    itemstack,
                    simulate == null ? internalSimulate : simulate);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        var setter = new HashMap<String, Method>();
        var clazz = getClass();
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("slot")) {
                if (port.getEdges().isEmpty()) {
                    try {
                        ConfiguratorParser.createFieldConfigurator(clazz.getField("internalSlot"), father, clazz, setter, this);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else if (port.fieldName.equals("simulate")) {
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
