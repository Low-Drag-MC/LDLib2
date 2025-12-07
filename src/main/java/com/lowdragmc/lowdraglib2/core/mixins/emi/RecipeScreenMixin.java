package com.lowdragmc.lowdraglib2.core.mixins.emi;

import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIWidget;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeScreen.class)
public abstract class RecipeScreenMixin extends Screen {
    @Shadow(remap = false) private List<WidgetGroup> currentPage;

    protected RecipeScreenMixin(Component title) {
        super(title);
    }

    @Override
    public void tick() {
        super.tick();
        for (var group : currentPage) {
            for (var widget : group.widgets) {
                if (widget instanceof ModularUIEMIWidget modularUIWidget) {
                    modularUIWidget.modularUI.tick();
                }
            }
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        for (var group : currentPage) {
            for (var widget : group.widgets) {
                if (widget instanceof ModularUIEMIWidget modularUIWidget) {
                    var ox = mouseX - group.x();
                    var oy = mouseY - group.y();
                    if (modularUIWidget.getBounds().contains((int) ox, (int) oy)) {
                        modularUIWidget.modularUI.getWidget().mouseMoved(ox, oy);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        for (var group : currentPage) {
            for (var widget : group.widgets) {
                if (widget instanceof ModularUIEMIWidget modularUIWidget) {
                    var ox = mouseX - group.x();
                    var oy = mouseY - group.y();
                    if (modularUIWidget.getBounds().contains((int) ox, (int) oy) &&
                            modularUIWidget.modularUI.getWidget().mouseClicked(ox, oy, button)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        for (var group : currentPage) {
            for (var widget : group.widgets) {
                if (widget instanceof ModularUIEMIWidget modularUIWidget) {
                    var ox = mouseX - group.x();
                    var oy = mouseY - group.y();
                    if (modularUIWidget.getBounds().contains((int) ox, (int) oy) &&
                            modularUIWidget.modularUI.getWidget().mouseReleased(ox, oy, button)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        for (var group : currentPage) {
            for (var widget : group.widgets) {
                if (widget instanceof ModularUIEMIWidget modularUIWidget) {
                    var ox = mouseX - group.x();
                    var oy = mouseY - group.y();
                    if (modularUIWidget.getBounds().contains((int) ox, (int) oy) &&
                            modularUIWidget.modularUI.getWidget().mouseDragged(ox, oy, button, deltaX, deltaY)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        for (var group : currentPage) {
            for (var widget : group.widgets) {
                if (widget instanceof ModularUIEMIWidget modularUIWidget) {
                    var ox = mouseX - group.x();
                    var oy = mouseY - group.y();
                    if (modularUIWidget.getBounds().contains((int) ox, (int) oy) &&
                            modularUIWidget.modularUI.getWidget().mouseScrolled(ox, oy, horizontal, vertical)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (var widget : widgetGroup.widgets) {
                if (widget instanceof ModularUIEMIWidget modularUIWidget) {
                    if (modularUIWidget.modularUI.getWidget().keyPressed(keyCode, scanCode, modifiers)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
