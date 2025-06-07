package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.block;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.trigger.LinearTriggerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

@LDLRegister(name = "place block", group = "graph_processor.node.minecraft.block", registry = "ldlib2:graph_node")
public class PlaceBockNode extends LinearTriggerNode {
    @InputPort
    public Level level;
    @InputPort
    public Vector3f xyz;
    @InputPort(name = "blockstate")
    public BlockState blockState;

    @Override
    public void process() {
        if (level != null && xyz != null && blockState != null) {
            level.setBlockAndUpdate(new BlockPos((int) xyz.x, (int) xyz.y, (int) xyz.z), blockState);
        }
    }
}
