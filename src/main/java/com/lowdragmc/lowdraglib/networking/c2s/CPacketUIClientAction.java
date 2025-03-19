package com.lowdragmc.lowdraglib.networking.c2s;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.NoArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@NoArgsConstructor
public class CPacketUIClientAction implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib.location("ui_client_action");
    public static final Type<CPacketUIClientAction> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketUIClientAction> CODEC = StreamCodec.ofMember(CPacketUIClientAction::write, CPacketUIClientAction::decode);
    public int windowId;
    public byte[] updateData;

    public CPacketUIClientAction(int windowId, byte[] updateData) {
        this.windowId = windowId;
        this.updateData = updateData;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(windowId);
        buf.writeByteArray(updateData);
    }

    public static CPacketUIClientAction decode(RegistryFriendlyByteBuf buf) {
        var windowId = buf.readVarInt();
        var updateData = buf.readByteArray();
        return new CPacketUIClientAction(windowId, updateData);
    }

    public static void execute(CPacketUIClientAction packet, IPayloadContext context) {
        AbstractContainerMenu openContainer = context.player().containerMenu;
        if (openContainer instanceof ModularUIContainer) {
            ((ModularUIContainer)openContainer).handleClientAction(packet);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
