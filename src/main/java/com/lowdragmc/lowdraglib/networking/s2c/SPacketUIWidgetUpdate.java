package com.lowdragmc.lowdraglib.networking.s2c;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@NoArgsConstructor
public class SPacketUIWidgetUpdate implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib.id("ui_widget_update");
    public static final Type<SPacketUIWidgetUpdate> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketUIWidgetUpdate> CODEC = StreamCodec.ofMember(SPacketUIWidgetUpdate::write, SPacketUIWidgetUpdate::decode);

    public int windowId;
    public byte[] updateData;

    public SPacketUIWidgetUpdate(int windowId, byte[] updateData) {
        this.windowId = windowId;
        this.updateData = updateData;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(windowId);
        buf.writeByteArray(updateData);
    }

    public static SPacketUIWidgetUpdate decode(RegistryFriendlyByteBuf buf) {
        var windowId = buf.readVarInt();
        var updateData = buf.readByteArray();
        return new SPacketUIWidgetUpdate(windowId, updateData);
    }

    public static void execute(SPacketUIWidgetUpdate packet, IPayloadContext context) {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof ModularUIGuiContainer) {
            ((ModularUIGuiContainer) currentScreen).handleWidgetUpdate(packet);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
