package com.lowdragmc.lowdraglib2.graphprocessor.nodes.minecraft.block;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib2.utils.Vector3fHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

@LDLRegister(name = "get block", group = "graph_processor.node.minecraft.block", registry = "ldlib2:graph_node")
public class BlockEntityGetNode extends BaseNode {
    @InputPort
    public Level level;
    @InputPort
    public Vector3f xyz;
    @OutputPort(name = "blockstate")
    public BlockState blockState;
    @OutputPort(name = "blockentity")
    public BlockEntity blockEntity;

    @Override
    public void process() {
        if (level != null && xyz != null) {
            var pos = Vector3fHelper.toBlockPos(xyz);
            blockState = level.getBlockState(pos);
            blockEntity = level.getBlockEntity(pos);
        }
    }
}
