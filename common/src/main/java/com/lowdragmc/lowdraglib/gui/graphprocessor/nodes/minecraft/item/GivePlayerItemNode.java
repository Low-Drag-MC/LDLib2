package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.runtime.ConfiguratorParser;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

@LDLRegister(name = "give player item", group = "graph_processor.node.minecraft.item")
public class GivePlayerItemNode extends LinearTriggerNode {
    @InputPort
    public Object target;
    @InputPort
    public ItemStack itemstack;
    @InputPort(name = "preferred slot", tips = "If the inventory can't hold it, the item will be dropped in the world at the players position.")
    public Integer preferredSlot;
    @Configurable(name = "preferred slot")
    public int internalPreferredSlot;

    @Override
    public void process() {
        if (target instanceof Player player && itemstack != null) {
            ItemTransferHelper.giveItemToPlayer(player, itemstack, preferredSlot == null ? internalPreferredSlot : preferredSlot);
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
