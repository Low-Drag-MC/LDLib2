package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.item;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.lowdraglib2.utils.Vector3fHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3f;

@LDLRegister(name = "drop item", group = "graph_processor.node.minecraft.item", registry = "ldlib2:graph_node")
public class DropItemNode extends LinearTriggerNode {
    @InputPort
    public Level level;
    @InputPort
    public Vector3f xyz;
    @InputPort(name = "item")
    public ItemStack itemStack;
    @InputPort(name = "direction", tips = "pop resource from face")
    public Direction direction;

    @Override
    public void process() {
        if (level != null && xyz != null && itemStack != null) {
            if (direction == null) {
                Block.popResource(level, Vector3fHelper.toBlockPos(xyz), itemStack);
            } else {
                Block.popResourceFromFace(level, Vector3fHelper.toBlockPos(xyz), direction, itemStack);
            }
        }
    }
}
