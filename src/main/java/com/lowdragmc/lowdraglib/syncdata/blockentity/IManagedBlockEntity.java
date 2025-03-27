package com.lowdragmc.lowdraglib.syncdata.blockentity;

import com.lowdragmc.lowdraglib.syncdata.storage.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.ref.IRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface IManagedBlockEntity {

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

    default IRef[] getNonLazyFields() {
        return getRootStorage().getNonLazyFields();
    }

    /**
     * Get the managed storage
     */
    IManagedStorage getRootStorage();
}
