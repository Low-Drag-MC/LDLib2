package com.lowdragmc.lowdraglib.fabric.core.mixins;

import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow protected EditBox input;

    @Inject(method = "handleChatInput", at = @At(value = "RETURN"), cancellable = true)
    private void injectHandleChatInput(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && Minecraft.getInstance().screen != (Object) this) {
            if (Minecraft.getInstance().screen instanceof ModularUIGuiContainer) {
                cir.setReturnValue(false);
            }
        }
    }
}
