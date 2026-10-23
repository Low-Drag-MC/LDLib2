package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SyncTestBlock extends Block implements EntityBlock {
    public static SyncTestBlock BLOCK;

    public SyncTestBlock(Properties properties) {
        super(properties.destroyTime(2));
        BLOCK = this;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SyncTestBlockEntity(pos, state);
    }
}
