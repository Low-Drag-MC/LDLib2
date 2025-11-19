package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.c2s.CPacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketUIRPCEventReturn;
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
        registrar.playToClient(SPacketUIRPCEventReturn.TYPE, SPacketUIRPCEventReturn.CODEC, SPacketUIRPCEventReturn::execute);

        registrar.playToServer(CPacketUIRPCEvent.TYPE, CPacketUIRPCEvent.CODEC, CPacketUIRPCEvent::execute);

        registrar.playBidirectional(PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC, PacketRPCBlockEntity::execute);
        registrar.playBidirectional(PacketModularUISync.TYPE, PacketModularUISync.CODEC, PacketModularUISync::execute);
    }

}
