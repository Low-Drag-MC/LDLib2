package com.lowdragmc.lowdraglib2.syncdata.holder.blockentity;

import com.lowdragmc.lowdraglib2.syncdata.holder.IManagedHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.NotImplementedException;

public interface IBlockEntityManagedHolder extends IManagedHolder {
    default BlockEntity getSelf() {
        if (this instanceof BlockEntity) {
            return (BlockEntity) this;
        } else {
            throw new NotImplementedException("This method should return a block entity");
        }
    }

    @Override
    default ChunkPos getTrackingPos() {
        return new ChunkPos(getSelf().getBlockPos());
    }

    @Override
    default ServerLevel getServerLevel() {
        if (getSelf().getLevel() instanceof ServerLevel serverLevel) {
            return serverLevel;
        }
        throw new IllegalStateException("BlockEntity is not on a server");
    }
}
