package com.lowdragmc.lowdraglib2.syncdata.holder;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCMethodMeta;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;
import java.util.Arrays;

public interface IRPCManagedHolder extends IManagedHolder {

    CustomPacketPayload createRPCPacket(byte[] data);

    /**
     * Get the RPC method
     */
    @Nullable
    default RPCMethodMeta getRPCMethod(IManaged managed, String methodName) {
        return managed.getFieldHolder().getRpcMethodMap().get(methodName);
    }

    default byte[] parseArgs2Bytes(IManaged managed, String methodName, Object... args) {
        var index = Arrays.stream(getRootStorage().getManaged()).toList().indexOf(managed);
        if (index < 0) {
            throw new IllegalArgumentException("No such rpc managed: " + methodName);
        }
        var rpcMethod = getRPCMethod(managed, methodName);
        if (rpcMethod == null) {
            throw new IllegalArgumentException("No such RPC method: " + methodName);
        }
        return ByteBufUtil.writeCustomData(buf -> {
            buf.writeVarInt(index);
            buf.writeUtf(methodName);
            rpcMethod.serializeArgs(buf, args);
        }, Platform.getFrozenRegistry());
    }

    @OnlyIn(Dist.CLIENT)
    default void rpcToServer(IManaged managed, String methodName, Object... args) {
        var packet = createRPCPacket(parseArgs2Bytes(managed, methodName, args));
        PacketDistributor.sendToServer(packet);
    }

    default void rpcToPlayer(IManaged managed, ServerPlayer player, String methodName, Object... args) {
        var packet = createRPCPacket(parseArgs2Bytes(managed, methodName, args));
        PacketDistributor.sendToPlayer(player, packet);
    }

    default void rpcToTracking(IManaged managed, String methodName, Object... args) {
        var packet = createRPCPacket(parseArgs2Bytes(managed, methodName, args));
        PacketDistributor.sendToPlayersTrackingChunk(getServerLevel(), getTrackingPos(), packet);
    }

    default void handleRPCPacket(RPCSender sender, byte[] data) {
        ByteBufUtil.readCustomData(data, buf -> {
            var index = buf.readVarInt();
            var methodName = buf.readUtf();

            var managed = getRootStorage().getManaged()[index];
            var rpcMethod = getRPCMethod(managed, methodName);
            if (rpcMethod == null) {
                LDLib2.LOGGER.error("Cannot find RPC method: {}, which is sent by {}", methodName, sender);
            } else {
                try {
                    rpcMethod.invoke(managed, sender, buf);
                } catch (Exception e) {
                    LDLib2.LOGGER.error("Error invoking RPC method: {}, which is sent by {}", methodName, sender, e);
                }
            }
        }, Platform.getFrozenRegistry());

    }
}
