package com.lowdragmc.lowdraglib2.syncdata.holder.blockentity;

import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.holder.ISyncMangedHolder;
import net.minecraft.nbt.CompoundTag;

import java.util.BitSet;

public interface ISyncBlockEntity extends ISyncMangedHolder, IBlockEntityManagedHolder {
    @Override
    default SPacketAutoSyncBlockEntity createSyncPacket(BitSet changed, byte[] data, CompoundTag extra) {
        return SPacketAutoSyncBlockEntity.of(this, changed, data, extra);
    }
}
