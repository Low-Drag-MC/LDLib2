package com.lowdragmc.lowdraglib2.syncdata;

import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.Consumer;

/**
 * Interface for block entities that are managed by the sync system.
 */
public interface IBlockEntityManaged extends IManaged {

    /**
     * @return the block entity that is managed by the sync system
     */
    default BlockEntity getManagedBlockEntity() {
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
        var blockEntity = getManagedBlockEntity();
        var level = blockEntity.getLevel();
        if (level != null) {
            if (level.isClientSide) {
                var state = blockEntity.getBlockState();
                level.sendBlockUpdated(blockEntity.getBlockPos(), state, state, 1 << 3);
            }
        }
    }

    @Override
    default void onChanged() {
        getManagedBlockEntity().setChanged();
    }

}
