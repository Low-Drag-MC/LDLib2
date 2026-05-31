package com.lowdragmc.lowdraglib2.syncdata.holder.blockentity;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.NotImplementedException;

import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * Interface for block entities that are managed by the sync system.
 */
public interface IBlockEntityManaged extends IManaged {
    /**
     * @return the block entity that is managed by the sync system
     */
    default BlockEntity asBlockEntity() {
        if (this instanceof BlockEntity) {
            return (BlockEntity) this;
        } else {
            throw new NotImplementedException("This method should return a block entity");
        }
    }

    default Consumer<Object> onRerenderTriggered(ManagedKey managedKey, Object currentValue) {
        return _ignored -> scheduleRenderUpdate();
    }

    /**
     * Called when a sync field is annotated as {@link com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender}
     */
    default void scheduleRenderUpdate() {
        var blockEntity = asBlockEntity();
        var level = blockEntity.getLevel();
        if (level != null) {
            if (level.isClientSide) {
                var state = blockEntity.getBlockState();
                level.sendBlockUpdated(blockEntity.getBlockPos(), state, state, 1 << 3);
            }
        }
    }

    @Override
    default void notifyPersistence() {
        var blockEntity = asBlockEntity();
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            var server = serverLevel.getServer();
            if (!Platform.serverSafe(server)) return;
            try {
                server.executeIfPossible(() -> {
                    if (Platform.serverSafe(server)) {
                        blockEntity.setChanged();
                    }
                });
            } catch (RejectedExecutionException ignored) {
                // The server can begin shutting down between the safety check and task submission.
            }
        }
    }
}
