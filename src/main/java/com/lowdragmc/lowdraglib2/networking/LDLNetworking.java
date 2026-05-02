package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEventReturn;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Author: KilaBash
 * Date: 2022/04/27
 * Description:
 */
public class LDLNetworking {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(LDLib2.MOD_ID);

        registrar.playToClient(SPacketAutoSyncBlockEntity.TYPE, SPacketAutoSyncBlockEntity.CODEC, SPacketAutoSyncBlockEntity::execute);

        registrar.playBidirectional(PacketUIRPCEvent.TYPE, PacketUIRPCEvent.CODEC, PacketUIRPCEvent::execute);
        registrar.playBidirectional(PacketUIRPCEventReturn.TYPE, PacketUIRPCEventReturn.CODEC, PacketUIRPCEventReturn::execute);

        registrar.playBidirectional(PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC, PacketRPCBlockEntity::execute);
        registrar.playBidirectional(PacketModularUISync.TYPE, PacketModularUISync.CODEC, PacketModularUISync::execute);

        registrar.playBidirectional(PacketRPCPacket.TYPE, PacketRPCPacket.CODEC, PacketRPCPacket::execute);
    }

}
