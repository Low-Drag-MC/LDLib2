package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor_outdated.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.utils.ConfiguratorParser;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.HashMap;

@LDLRegister(name = "give player item", group = "graph_processor.node.minecraft.item", registry = "ldlib:graph_node")
public class GivePlayerItemNode extends LinearTriggerNode {
    @InputPort
    public Object target;
    @InputPort
    public ItemStack itemstack;
    @InputPort(name = "preferred slot", tips = "If the inventory can't hold it, the item will be dropped in the world at the players position.")
    public Integer preferredSlot;
    @OutputPort(name = "item transfer")
    public IItemHandler itemTransfer;
    @Configurable(name = "preferred slot")
    public int internalPreferredSlot;

    @Override
    public void process() {
        if (target instanceof Player player && itemstack != null) {
            ItemHandlerHelper.giveItemToPlayer(player, itemstack, preferredSlot == null ? internalPreferredSlot : preferredSlot);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        var clazz = getClass();
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("preferredSlot")) {
                if (port.getEdges().isEmpty()) {
                    try {
                        ConfiguratorParser.createFieldConfigurator(clazz.getField("internalPreferredSlot"), father, clazz, new HashMap<>(), this);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
