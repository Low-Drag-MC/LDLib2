package com.lowdragmc.lowdraglib2.networking.both;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.IUISyncManagerHolder;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
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
public class PacketModularUISync implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("modular_ui_sync");
    public static final Type<PacketModularUISync> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketModularUISync> CODEC = StreamCodec.ofMember(PacketModularUISync::write, PacketModularUISync::decode);

    private byte[] data;

    public PacketModularUISync(byte[] data) {
        this.data = data;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeByteArray(data);
    }

    public static PacketModularUISync decode(RegistryFriendlyByteBuf buffer) {
        var data = buffer.readByteArray();
        return new PacketModularUISync(data);
    }

    public static void handle(PacketModularUISync packet, Player player, RegistryAccess registryAccess) {
        if (player instanceof ServerPlayer) {
            executeServer(packet, player, registryAccess);
        } else {
            executeClient(packet, player, registryAccess);
        }
    }

    public static void executeClient(PacketModularUISync packet, Player player, RegistryAccess registryAccess) {
        if (player.containerMenu instanceof IUISyncManagerHolder syncManagerHolder) {
            var syncManager = syncManagerHolder.getSyncManager();
            if (syncManager == null) return;
            ByteBufUtil.readCustomData(packet.data,
                    syncManager::handleSyncPacket,
                    registryAccess);
        }
    }

    public static void executeServer(PacketModularUISync packet, Player player, RegistryAccess registryAccess) {
        if (player.containerMenu instanceof IUISyncManagerHolder syncManagerHolder) {
            var syncManager = syncManagerHolder.getSyncManager();
            if (syncManager == null) return;
            ByteBufUtil.readCustomData(packet.data,
                    syncManager::handleSyncPacket,
                    registryAccess);
        }
    }

    @Override
    @Nonnull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
