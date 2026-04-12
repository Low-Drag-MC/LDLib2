package com.lowdragmc.lowdraglib2.networking.c2s;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.IUISyncManagerHolder;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.RegistryAccess;

@NoArgsConstructor
public class CPacketUIRPCEvent implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("ui_rpc_event");
    public static final Type<CPacketUIRPCEvent> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketUIRPCEvent> CODEC = StreamCodec.ofMember(CPacketUIRPCEvent::write, CPacketUIRPCEvent::decode);
    public byte[] eventData;

    public CPacketUIRPCEvent(byte[] eventData) {
        this.eventData = eventData;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeByteArray(eventData);
    }

    public static CPacketUIRPCEvent decode(RegistryFriendlyByteBuf buf) {
        var eventData = buf.readByteArray();
        return new CPacketUIRPCEvent(eventData);
    }

    public static void handle(CPacketUIRPCEvent packet, Player player, RegistryAccess registryAccess) {
        if (player.containerMenu instanceof IUISyncManagerHolder syncManagerHolder) {
            var syncManager = syncManagerHolder.getSyncManager();
            if (syncManager == null) return;
            ByteBufUtil.readCustomData(packet.eventData,
                    syncManager::handEvent,
                    registryAccess);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
