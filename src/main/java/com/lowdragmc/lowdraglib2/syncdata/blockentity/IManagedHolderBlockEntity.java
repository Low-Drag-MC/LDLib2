package com.lowdragmc.lowdraglib2.syncdata.blockentity;

import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface IManagedHolderBlockEntity {

    /**
     * @return the block entity type
     */
    default BlockEntityType<?> getBlockEntityType() {
        return getSelf().getType();
    }

    /**
     * Get the position of this block entity, used to identify it.
     */
    default BlockPos getCurrentPos() {
        return getSelf().getBlockPos();
    }

    /**
     * @return the BlockEntity itself
     */
    default BlockEntity getSelf() {
        return (BlockEntity) this;
    }

    /**
     * Get all fields that are not lazy loaded.
     */
    default IRef[] getNonLazyFields() {
        return getRootStorage().getNonLazyFields();
    }

    /**
     * Get the managed storage
     */
    IManagedStorage getRootStorage();
}
