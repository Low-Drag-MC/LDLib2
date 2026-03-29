package com.lowdragmc.lowdraglib2.syncdata.holder.blockentity;

import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.holder.IRPCManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCMethodMeta;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import org.jetbrains.annotations.Nullable;

public interface IRPCBlockEntity extends IRPCManagedHolder, IBlockEntityManagedHolder {
    @Override
    default PacketRPCBlockEntity createRPCPacket(byte[] data) {
        return PacketRPCBlockEntity.of(this, data);
    }
}
