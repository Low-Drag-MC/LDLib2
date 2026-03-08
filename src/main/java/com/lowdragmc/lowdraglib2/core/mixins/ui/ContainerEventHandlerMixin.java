package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin extends GuiEventListener{

    @Shadow
    List<? extends GuiEventListener> children();

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
