package com.lowdragmc.lowdraglib2.networking.both;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.IUISyncManagerHolder;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@NoArgsConstructor
public class PacketUIRPCEvent implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("ui_rpc_event");
    public static final Type<PacketUIRPCEvent> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUIRPCEvent> CODEC = StreamCodec.ofMember(PacketUIRPCEvent::write, PacketUIRPCEvent::decode);
    public byte[] eventData;

    public PacketUIRPCEvent(byte[] eventData) {
        this.eventData = eventData;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeByteArray(eventData);
    }

    public static PacketUIRPCEvent decode(RegistryFriendlyByteBuf buf) {
        var eventData = buf.readByteArray();
        return new PacketUIRPCEvent(eventData);
    }

    public static void execute(PacketUIRPCEvent packet, IPayloadContext context) {
        var player = context.player();
        if (player.containerMenu instanceof IUISyncManagerHolder syncManagerHolder) {
            var syncManager = syncManagerHolder.getSyncManager();
            if (syncManager == null) return;
            ByteBufUtil.readCustomData(packet.eventData,
                    syncManager::handEvent,
                    context.player().registryAccess());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
