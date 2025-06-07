package com.lowdragmc.lowdraglib2.syncdata.blockentity;

import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCMethodMeta;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

public interface IRPCBlockEntity extends IManagedBlockEntity {

    /**
     * Get the RPC method
     */
    @Nullable
    default RPCMethodMeta getRPCMethod(IManaged managed, String methodName) {
        return managed.getFieldHolder().getRpcMethodMap().get(methodName);
    }

    default PacketRPCBlockEntity generateRpcPacket(IManaged managed, String methodName,Object... args) {
        return PacketRPCBlockEntity.of(managed, this, methodName, args);
    }

    @OnlyIn(Dist.CLIENT)
    default void rpcToServer(IManaged managed, String methodName, Object... args) {
        var packet = generateRpcPacket(managed, methodName, args);
        PacketDistributor.sendToServer(packet);
    }

    default void rpcToPlayer(IManaged managed, ServerPlayer player, String methodName, Object... args) {
        var packet = generateRpcPacket(managed, methodName, args);
        PacketDistributor.sendToPlayer(player, packet);
    }

    default void rpcToTracking(IManaged managed, String methodName, Object... args) {
        var packet = generateRpcPacket(managed, methodName, args);
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) getSelf().getLevel(), new ChunkPos(getCurrentPos()), packet);
    }

}
