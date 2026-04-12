package com.lowdragmc.lowdraglib2.networking.rpc;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCMethodMeta;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public final class RPCPacketDistributor {
    private final Map<String, RPCPacketHandler> RPC_PACKETS = new ConcurrentHashMap<>();

    public void init() {
        ReflectionUtils.findAnnotationStaticMethod(RPCPacket.class, data -> {
            var modId = data.getOrDefault("modId", "").toString();
            if (modId.isEmpty()) return true;
            return LDLib2.isModLoaded(modId);
        }, method -> {
            var rpcPacket = method.getAnnotation(RPCPacket.class);
            var packetID = rpcPacket.value();
            method.setAccessible(true);
            var rpcMethod = new RPCMethodMeta(method);
            registerPacket(packetID, rpcMethod);
        }, () -> {});
    }

    @Nullable
    public RPCPacketHandler getPacketHandler(String packetID) {
        return RPC_PACKETS.get(packetID);
    }

    public RPCPacketHandler getSafePacketHandler(String packetID) {
        var handler = getPacketHandler(packetID);
        if (handler == null) {
            LDLib2.LOGGER.warn("Received rpc packet with no registered handler: {}", packetID);
            handler = RPCPacketHandler.EMPTY;
        }
        return handler;
    }

    public void registerPacket(String packetID, RPCPacketHandler handler) {
        if (RPC_PACKETS.containsKey(packetID)) throw new IllegalArgumentException("Packet ID '" + packetID + "' is already registered!");
        RPC_PACKETS.put(packetID, handler);
    }

    public void rpcToServer(String packetID, Object... args) {
        var data = getSafePacketHandler(packetID).args2Bytes(args);
        ClientPlayNetworking.send(PacketRPCPacket.of(packetID, data));
    }

    public void rpcToPlayer(ServerPlayer player, String packetID, Object... args) {
        var data = getSafePacketHandler(packetID).args2Bytes(args);
        ServerPlayNetworking.send(player, PacketRPCPacket.of(packetID, data));
    }

    public void rpcToAllPlayers(String packetID, Object... args) {
        var server = Platform.getMinecraftServer();
        if (server == null) return;
        var data = getSafePacketHandler(packetID).args2Bytes(args);
        var packet = PacketRPCPacket.of(packetID, data);
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    public void rpcToTracking(ServerLevel level, ChunkPos chunkPos, String packetID, Object... args) {
        var data = getSafePacketHandler(packetID).args2Bytes(args);
        var packet = PacketRPCPacket.of(packetID, data);
        for (ServerPlayer player : PlayerLookup.tracking(level, chunkPos)) {
            ServerPlayNetworking.send(player, packet);
        }
    }

}
