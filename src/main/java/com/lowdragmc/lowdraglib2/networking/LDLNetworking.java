package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.c2s.CPacketUIClientAction;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketUIOpen;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketUIWidgetUpdate;
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

        registrar.playToClient(SPacketUIOpen.TYPE, SPacketUIOpen.CODEC, SPacketUIOpen::execute);
        registrar.playToClient(SPacketUIWidgetUpdate.TYPE, SPacketUIWidgetUpdate.CODEC, SPacketUIWidgetUpdate::execute);
        registrar.playToClient(SPacketAutoSyncBlockEntity.TYPE, SPacketAutoSyncBlockEntity.CODEC, SPacketAutoSyncBlockEntity::execute);

        registrar.playToServer(CPacketUIClientAction.TYPE, CPacketUIClientAction.CODEC, CPacketUIClientAction::execute);

        registrar.playBidirectional(PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC, PacketRPCBlockEntity::execute);
    }

}
