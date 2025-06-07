package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.entity;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib2.misc.PlayerInventoryTransfer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;

@LDLRegister(name = "player info", group = "graph_processor.node.minecraft.entity", registry = "ldlib2:graph_node")
public class PlayerInfoNode extends BaseNode {
    @InputPort
    public Object in;
    @OutputPort
    public IItemHandler inventory;
    @OutputPort(name = "is crouching")
    public boolean isCrouching;
    @OutputPort
    public String name;
    @OutputPort
    public int xp;

    @Override
    public void process() {
        if (in instanceof Player player) {
            inventory = new PlayerInventoryTransfer(player.getInventory());
            isCrouching = player.isCrouching();
            name = player.getName().getString();
            xp = player.totalExperience;
        }
    }
}
