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
        PayloadRegistrar registar = event.registrar(LDLib2.MOD_ID);

        registar.playToClient(SPacketUIOpen.TYPE, SPacketUIOpen.CODEC, SPacketUIOpen::execute);
        registar.playToClient(SPacketUIWidgetUpdate.TYPE, SPacketUIWidgetUpdate.CODEC, SPacketUIWidgetUpdate::execute);
        registar.playToClient(SPacketAutoSyncBlockEntity.TYPE, SPacketAutoSyncBlockEntity.CODEC, SPacketAutoSyncBlockEntity::execute);

        registar.playToServer(CPacketUIClientAction.TYPE, CPacketUIClientAction.CODEC, CPacketUIClientAction::execute);

        registar.playBidirectional(PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC, PacketRPCBlockEntity::execute);
    }

}
