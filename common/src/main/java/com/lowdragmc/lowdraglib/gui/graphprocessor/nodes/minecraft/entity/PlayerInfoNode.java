package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.minecraft.entity;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.misc.ContainerTransfer;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import net.minecraft.world.entity.player.Player;

@LDLRegister(name = "player info", group = "graph_processor.node.minecraft.entity")
public class PlayerInfoNode extends BaseNode {
    @InputPort
    public Object in;
    @OutputPort
    public IItemTransfer inventory;
    @OutputPort(name = "is crouching")
    public boolean isCrouching;
    @OutputPort
    public String name;
    @OutputPort
    public int xp;

    @Override
    public void process() {
        if (in instanceof Player player) {
            inventory = new ContainerTransfer(player.getInventory());
            isCrouching = player.isCrouching();
            name = player.getName().getString();
            xp = player.totalExperience;
        }
    }
}
