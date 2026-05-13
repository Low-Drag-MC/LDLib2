package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin extends GuiEventListener{

    @Shadow
    List<? extends GuiEventListener> children();

    @Shadow
    void setFocused(final @Nullable GuiEventListener focused);

    @Shadow
    void setDragging(boolean dragging);

    @Shadow
    Optional<GuiEventListener> getChildAt(double x, double y);

    @Inject(method = "mouseClicked", at = @At(value = "HEAD", target = "Ljava/util/Optional;get()Ljava/lang/Object;"),
            cancellable = true)
    private void ldlib2$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        // because vanilla logic will always return true if the mouse is over a widget,
        // we need to check if the mouse is over the widget itself, meanwhile we can still return false if the widget is not clicked
        for (var child : children()) {
            if (child instanceof IModularUIHolder holder) {
                var mui = holder.getModularUI();
                if (mui == null) continue;
                var widget = ModularUIClientAccess.getWidget(mui);
                if (widget.mouseClicked(event, doubleClick)) {
                    if (child.shouldTakeFocusAfterInteraction()) {
                        setFocused(widget);
                        if (event.button() == 0) {
                            setDragging(true);
                        }
                    }
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

    @Inject(method = "mouseDragged", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        for (var child : children()) {
            if (child instanceof IModularUIHolder holder) {
                var mui = holder.getModularUI();
                if (mui != null && ModularUIClientAccess.getWidget(mui).mouseDragged(event, dx, dy)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Override
    default void mouseMoved(double mouseX, double mouseY) {
        for (var child : children()) {
            if (child instanceof IModularUIHolder holder) {
                var mui = holder.getModularUI();
                if (mui == null) continue;
                ModularUIClientAccess.getWidget(mui).mouseMoved(mouseX, mouseY);
            }
        }
    }
}
