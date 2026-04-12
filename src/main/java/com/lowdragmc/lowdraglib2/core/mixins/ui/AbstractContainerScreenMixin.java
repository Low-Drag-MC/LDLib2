package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.holder.IItemSlotHolderMenu;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> implements ContainerEventHandler {

    @Shadow
    public abstract T getMenu();
    
    @Shadow
    protected Slot hoveredSlot;

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

    @Inject(method = "mouseDragged", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (getMenu() instanceof IModularUIHolder holder) {
            var mui = holder.getModularUI();
            if (mui != null && mui.getWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "containerTick", at = @At(value = "RETURN"))
    private void ldlib2$containerTick(CallbackInfo ci) {
        if (getMenu() instanceof IModularUIHolder holder) {
            var mui = holder.getModularUI();
            if (mui == null) return;
            mui.syncManager.tick();
        }
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$isHovering(Slot slot, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (getMenu() instanceof IModularUIHolder holder) {
            var mui = holder.getModularUI();
            if (mui == null) return;
            if (getMenu() instanceof IItemSlotHolderMenu menu) {
                if (!menu.isItemSlot(slot)) return;
            }
            if (mui.getLastHoveredElement() instanceof ItemSlot itemSlot && itemSlot.getSlot() == slot) {
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"))
    private void ldlib2$renderSlotHighlight(GuiGraphics guiGraphics, int x, int y, int z, Operation<Void> original) {
        if (getMenu() instanceof IItemSlotHolderMenu menu) {
            if (this.hoveredSlot != null && menu.isItemSlot(this.hoveredSlot)) {
                return;
            }
        }
        original.call(guiGraphics, x, y, z);
    }

    @Inject(method = "renderSlot", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$renderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (getMenu() instanceof IItemSlotHolderMenu menu) {
            if (menu.isItemSlot(slot)) {
                ci.cancel();
            }
        }
    }
}
