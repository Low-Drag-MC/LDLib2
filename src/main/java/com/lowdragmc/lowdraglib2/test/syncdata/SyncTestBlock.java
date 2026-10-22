package com.lowdragmc.lowdraglib2.test.syncdata;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SyncTestBlock extends Block implements EntityBlock {
    public static final SyncTestBlock BLOCK = new SyncTestBlock();

    private SyncTestBlock() {
        super(Properties.of().destroyTime(2));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SyncTestBlockEntity(pos, state);
    }
}
