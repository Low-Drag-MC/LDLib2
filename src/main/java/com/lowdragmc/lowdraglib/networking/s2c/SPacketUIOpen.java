package com.lowdragmc.lowdraglib.networking.s2c;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.utils.ByteBufUtil;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class SPacketUIOpen implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib.id("ui_open");
    public static final Type<SPacketUIOpen> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketUIOpen> CODEC = StreamCodec.ofMember(SPacketUIOpen::write, SPacketUIOpen::decode);
    private ResourceLocation uiFactoryId;
    private byte[] serializedDta;
    private int windowId;

    public SPacketUIOpen(ResourceLocation uiFactoryId, byte[] serializedDta, int windowId) {
        this.uiFactoryId = uiFactoryId;
        this.serializedDta = serializedDta;
        this.windowId = windowId;
    }

    public void write(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(uiFactoryId);
        buf.writeVarInt(windowId);
        buf.writeByteArray(serializedDta);
    }

    public static SPacketUIOpen decode(RegistryFriendlyByteBuf buf) {
        var uiFactoryId = buf.readResourceLocation();
        var windowId = buf.readVarInt();
        var data = buf.readByteArray();
        return new SPacketUIOpen(uiFactoryId, data, windowId);
    }

    public static void execute(SPacketUIOpen packet, IPayloadContext context) {
        UIFactory<?> uiFactory = UIFactory.FACTORIES.get(packet.uiFactoryId);
        if (uiFactory != null) {
            ByteBufUtil.readCustomData(packet.serializedDta,
                    buf -> uiFactory.initClientUI(buf, packet.windowId),
                    context.player().registryAccess());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
