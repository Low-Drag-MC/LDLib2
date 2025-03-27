package com.lowdragmc.lowdraglib.utils.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public record BlockPosFace(BlockPos pos, Direction facing) {

    @Override
    public boolean equals(@Nullable Object other) {
        if (other instanceof BlockPosFace(BlockPos pos1, Direction facing1)) {
            return pos.equals(pos1) && facing1 == facing;
        }
        return false;
    }

}
