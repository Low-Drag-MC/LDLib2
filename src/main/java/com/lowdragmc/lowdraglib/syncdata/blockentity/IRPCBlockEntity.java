package com.lowdragmc.lowdraglib.syncdata.blockentity;

import com.lowdragmc.lowdraglib.networking.both.PacketRPCMethodPayload;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.field.RPCMethodMeta;
import net.minecraft.core.HolderLookup;
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

    default PacketRPCMethodPayload generateRpcPacket(IManaged managed, String methodName, HolderLookup.Provider provider, Object... args) {
        return PacketRPCMethodPayload.of(managed, this, methodName, provider, args);
    }

    @OnlyIn(Dist.CLIENT)
    default void rpcToServer(IManaged managed, String methodName, HolderLookup.Provider provider, Object... args) {
        var packet = generateRpcPacket(managed, methodName, provider, args);
        PacketDistributor.sendToServer(packet);
    }


    default void rpcToPlayer(IManaged managed, ServerPlayer player, String methodName, HolderLookup.Provider provider, Object... args) {
        var packet = generateRpcPacket(managed, methodName, provider, args);
        PacketDistributor.sendToPlayer(player, packet);
    }

    default void rpcToTracking(IManaged managed, String methodName, HolderLookup.Provider provider, Object... args) {
        var packet = generateRpcPacket(managed, methodName, provider, args);
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) getSelf().getLevel(), new ChunkPos(getCurrentPos()), packet);
    }

}
