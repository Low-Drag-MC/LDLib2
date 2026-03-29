package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.c2s.CPacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketUIRPCEventReturn;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
public class LDLNetworking {

    
    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (net.fabricmc.api.EnvType.CLIENT == net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        }
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }

    public static void register() {
        // S2C
        PayloadTypeRegistry.playS2C().register(SPacketAutoSyncBlockEntity.TYPE, SPacketAutoSyncBlockEntity.CODEC);
        PayloadTypeRegistry.playS2C().register(SPacketUIRPCEventReturn.TYPE, SPacketUIRPCEventReturn.CODEC);
        
        // C2S
        PayloadTypeRegistry.playC2S().register(CPacketUIRPCEvent.TYPE, CPacketUIRPCEvent.CODEC);

        // Bidirectional (Registering in both registries as per Fabric 1.20.4+ / 1.21 requirements)
        PayloadTypeRegistry.playS2C().register(PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC);
        
        PayloadTypeRegistry.playS2C().register(PacketModularUISync.TYPE, PacketModularUISync.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketModularUISync.TYPE, PacketModularUISync.CODEC);
        
        PayloadTypeRegistry.playS2C().register(PacketRPCPacket.TYPE, PacketRPCPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketRPCPacket.TYPE, PacketRPCPacket.CODEC);
    }

}
