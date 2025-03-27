package com.lowdragmc.lowdraglib.core.mixins;

import com.lowdragmc.lowdraglib.compass.CompassManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {

    protected ClientPacketListenerMixin(Minecraft pMinecraft, Connection pConnection, CommonListenerCookie pCommonListenerCookie) {
        super(pMinecraft, pConnection, pCommonListenerCookie);
    }

    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void injectCreateReload(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        CompassManager.INSTANCE.onResourceManagerReload(this.minecraft.getResourceManager());
    }

}
