package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler implements ContainerEventHandler, GuiEventListener {
    @Shadow
    @org.jetbrains.annotations.Nullable
    protected Minecraft minecraft;

    @Shadow
    public abstract List<? extends GuiEventListener> children();

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void ldlib2$tick(CallbackInfo ci) {
        for (var child : children()) {
            if (child instanceof IModularUIHolder holder) {
                var mui = holder.getModularUI();
                if (mui != null) {
                    mui.tick();
                }
            }
        }
    }

    @Inject(method = "removed", at = @At(value = "RETURN"))
    private void ldlib2$removed(CallbackInfo ci) {
        for (var child : children()) {
            if (child instanceof IModularUIHolder holder) {
                var mui = holder.getModularUI();
                if (mui != null) {
                    mui.onRemoved();
                }
            }
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (var child : children()) {
            if (child instanceof IModularUIHolder holder) {
                var mui = holder.getModularUI();
                if (minecraft != null && mui != null) {
                    if (!mui.shouldCloseOnKeyInventory()) {
                        InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
                        if (minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
                            cir.setReturnValue(mui.getWidget().keyPressed(keyCode, scanCode, modifiers));
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "shouldCloseOnEsc", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$shouldCloseOnEsc(CallbackInfoReturnable<Boolean> cir) {
        for (var child : children()) {
            if (child instanceof IModularUIHolder holder) {
                var mui = holder.getModularUI();
                if (mui != null && !mui.shouldCloseOnEsc()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

}
