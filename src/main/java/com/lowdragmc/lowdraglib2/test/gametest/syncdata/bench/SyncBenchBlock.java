package com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SyncBenchBlock extends Block implements EntityBlock {
    public static SyncBenchBlock BLOCK;

    public SyncBenchBlock(Properties properties) {
        super(properties.destroyTime(2));
        BLOCK = this;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SyncBenchBlockEntity(pos, state);
    }
}
