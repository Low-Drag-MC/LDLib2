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

@LDLRegister(name = "item extract", group = "graph_processor.node.minecraft.item", registry = "ldlib:graph_node")
public class ItemTransferExtractNode extends LinearTriggerNode {
    @InputPort(name = "item transfer")
    public IItemHandler itemTransfer;
    @InputPort
    public Integer amount;
    @InputPort(name = "slot index")
    public Integer slot;
    @InputPort
    public Boolean simulate;
    @OutputPort
    public ItemStack extracted;
    @Configurable(name = "amount")
    public int internalAmount;
    @Configurable(name = "slot index")
    public int internalSlot;
    @Configurable(name = "simulate")
    public boolean internalSimulate;

    @Override
    public void process() {
        extracted = null;
        if (itemTransfer != null) {
            extracted = itemTransfer.extractItem(
                    slot == null ? internalSlot : slot,
                    amount == null ? internalAmount : amount,
                    simulate == null ? internalSimulate : simulate);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        var setter = new HashMap<String, Method>();
        var clazz = getClass();
        for (var port : getInputPorts()) {
            switch (port.fieldName) {
                case "amount" -> {
                    if (port.getEdges().isEmpty()) {
                        try {
                            ConfiguratorParser.createFieldConfigurator(clazz.getField("internalAmount"), father, clazz, setter, this);
                        } catch (NoSuchFieldException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                case "slot" -> {
                    if (port.getEdges().isEmpty()) {
                        try {
                            ConfiguratorParser.createFieldConfigurator(clazz.getField("internalSlot"), father, clazz, setter, this);
                        } catch (NoSuchFieldException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                case "simulate" -> {
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
}
