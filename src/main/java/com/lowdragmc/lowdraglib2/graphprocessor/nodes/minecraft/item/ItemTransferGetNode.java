package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.lowdraglib2.utils.Vector3fHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.joml.Vector3f;

@LDLRegister(name = "item transfer get", group = "graph_processor.node.minecraft.item", registry = "ldlib2:graph_node")
public class ItemTransferGetNode extends LinearTriggerNode {
    @InputPort
    public Level level;
    @InputPort
    public Vector3f xyz;
    @InputPort(name = "direction")
    public Direction direction;
    @OutputPort(name = "item transfer")
    public IItemHandler itemTransfer;

    @Override
    public void process() {
        if (level != null && xyz != null) {
            var pos = Vector3fHelper.toBlockPos(xyz);
            itemTransfer = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
        }
    }
}
