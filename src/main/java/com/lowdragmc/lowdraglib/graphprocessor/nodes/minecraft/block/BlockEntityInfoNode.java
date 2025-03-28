package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft.block;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@LDLRegister(name = "blockentity info", group = "graph_processor.node.minecraft.block", registry = "ldlib:graph_node")
public class BlockEntityInfoNode extends BaseNode {
    @InputPort
    public Object in;
    @OutputPort
    public Level level;
    @OutputPort
    public BlockPos pos;
    @OutputPort(name = "blockstate")
    public BlockState blockState;
    @OutputPort
    public CompoundTag tag;

    @Override
    public void process() {
        if (in instanceof BlockEntity be) {
            level = be.getLevel();
            pos = be.getBlockPos();
            blockState = be.getBlockState();
            tag = be.saveWithId(Platform.getFrozenRegistry());
        }
    }
}
