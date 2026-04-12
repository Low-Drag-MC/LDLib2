package com.lowdragmc.lowdraglib2.networking.s2c;

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

import javax.annotation.Nonnull;

@NoArgsConstructor
public class SPacketUIRPCEventReturn implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("ui_rpc_event_return");
    public static final Type<SPacketUIRPCEventReturn> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketUIRPCEventReturn> CODEC = StreamCodec.ofMember(SPacketUIRPCEventReturn::write, SPacketUIRPCEventReturn::decode);

    public byte[] returnData;

    public SPacketUIRPCEventReturn(byte[] returnData) {
        this.returnData = returnData;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeByteArray(returnData);
    }

    public static SPacketUIRPCEventReturn decode(RegistryFriendlyByteBuf buf) {
        var returnData = buf.readByteArray();
        return new SPacketUIRPCEventReturn(returnData);
    }

    public static void handle(SPacketUIRPCEventReturn packet, Player player, RegistryAccess registryAccess) {
        if (player.containerMenu instanceof IUISyncManagerHolder syncManagerHolder) {
            var syncManager = syncManagerHolder.getSyncManager();
            if (syncManager == null) return;
            ByteBufUtil.readCustomData(packet.returnData,
                    syncManager::handEventReturn,
                    registryAccess);
        }
    }

    @Override
    @Nonnull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
