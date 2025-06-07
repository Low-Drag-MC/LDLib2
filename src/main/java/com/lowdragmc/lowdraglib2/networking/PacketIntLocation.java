package com.lowdragmc.lowdraglib2.networking;

import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * a packet that contains a BlockPos
 */
@NoArgsConstructor
public abstract class PacketIntLocation implements CustomPacketPayload {
    protected BlockPos pos;

    public PacketIntLocation(BlockPos pos) {
        this.pos = pos;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }
}
