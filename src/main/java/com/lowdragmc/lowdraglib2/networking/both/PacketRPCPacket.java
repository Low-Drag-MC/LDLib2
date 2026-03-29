package com.lowdragmc.lowdraglib2.networking.both;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.RegistryAccess;

import javax.annotation.Nonnull;
@NoArgsConstructor
public class PacketRPCPacket implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("rpc_packet");
    public static final Type<PacketRPCPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRPCPacket> CODEC = StreamCodec.ofMember(PacketRPCPacket::write, PacketRPCPacket::decode);

    private String packetID;

    private byte[] data;

    public PacketRPCPacket(String packetID, byte[] data) {
        this.packetID = packetID;
        this.data = data;
    }

    public static PacketRPCPacket of(String packetID, byte[] data) {
        return new PacketRPCPacket(packetID, data);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(packetID);
        buf.writeByteArray(data);
    }

    public static PacketRPCPacket decode(RegistryFriendlyByteBuf buffer) {
        var packetID = buffer.readUtf();
        var data = buffer.readByteArray();
        return new PacketRPCPacket(packetID, data);
    }

    public static void handle(PacketRPCPacket packet, Player player, RegistryAccess registryAccess) {
        if (player instanceof ServerPlayer) {
            executeServer(packet, player, registryAccess);
        } else {
            executeClient(packet, player, registryAccess);
        }
    }

    public static void executeClient(PacketRPCPacket packet, Player player, RegistryAccess registryAccess) {
        if (player.level() == null) {
            return;
        }
        var handler = RPCPacketDistributor.getPacketHandler(packet.packetID);
        if (handler == null) {
            LDLib2.LOGGER.warn("Received rpc payload packet from server of a non registered handler: {}, which is an inconsistency between client and server.",
                    packet.packetID);
            return;
        }
        var sender = RPCSender.ofServer();
        var data = handler.bytes2Args(packet.data);
        handler.handler(sender, data);
    }

    public static void executeServer(PacketRPCPacket packet, Player player, RegistryAccess registryAccess) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LDLib2.LOGGER.error("Received rpc payload packet from client with no server player!");
            return;
        }

        var handler = RPCPacketDistributor.getPacketHandler(packet.packetID);
        if (handler == null) {
            LDLib2.LOGGER.warn("Received rpc payload packet from client sender {} of a non registered handler: {}, which may be an inconsistency between client and server, or even a potential attack!",
                    serverPlayer, packet.packetID);
            return;
        }
        var sender = RPCSender.ofClient(serverPlayer);
        var data = handler.bytes2Args(packet.data);
        handler.handler(sender, data);
    }

    @Override
    @Nonnull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
