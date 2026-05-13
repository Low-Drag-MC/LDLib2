package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.holder.IItemSlotHolderMenu;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> implements ContainerEventHandler {

    @Shadow
    public abstract T getMenu();

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    // cause forge call mouseReleased twice, ContainerEventHandlerMixin will also be called twice.
    // we skip it in advanced
    @Inject(method = "mouseReleased", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        for (var child : children()) {
            if (child instanceof IModularUIHolder holder) {
                var mui = holder.getModularUI();
                if (mui == null) continue;
                var widget = ModularUIClientAccess.getWidget(mui);
                if (widget.mouseReleased(event)) {
                    cir.setReturnValue(true);
                    return;
                }
                // if mouse clicked on the widget itself, ignore the event
                var hovered = getChildAt(event.x(), event.y());
                if (hovered.isPresent() && hovered.get() == widget) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        for (var child : children()) {
            if (child instanceof IModularUIHolder) {
                if (child.mouseScrolled(x, y, scrollX, scrollY)) {
                    cir.setReturnValue(true);
                    return;
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

    @Inject(method = "mouseDragged", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (getMenu() instanceof IModularUIHolder holder) {
            var mui = holder.getModularUI();
            if (mui != null && ModularUIClientAccess.getWidget(mui).mouseDragged(event, dx, dy)) {
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

    @Inject(method = "extractSlotHighlightBack", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$renderSlotHighlightBack(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (getMenu() instanceof IItemSlotHolderMenu menu) {
            if (menu.isItemSlot(this.hoveredSlot)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "extractSlotHighlightFront", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$renderSlotHighlightFront(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (getMenu() instanceof IItemSlotHolderMenu menu) {
            if (menu.isItemSlot(this.hoveredSlot)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "extractSlot", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$renderSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (getMenu() instanceof IItemSlotHolderMenu menu) {
            if (menu.isItemSlot(slot)) {
                ci.cancel();
            }
        }
    }
}
