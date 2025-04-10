package com.lowdragmc.lowdraglib.core.mixins.emi;

import com.lowdragmc.lowdraglib.integration.emi.ModularWrapperWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * @author KilaBash
 * @date 2023/4/3
 * @implNote WidgetGroupMixin
 */
@Mixin(RecipeScreen.class)
public abstract class RecipeScreenMixin {

    @Shadow(remap = false) private List<WidgetGroup> currentPage;

    @Inject(method = "mouseReleased", at = @At(value = "HEAD"), cancellable = true)
    private void initMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ModularWrapperWidget wrapperWidget) {
                    if (wrapperWidget.mouseReleased(mouseX, mouseY, button)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At(value = "HEAD"), cancellable = true)
    private void initMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ModularWrapperWidget wrapperWidget) {
                    if (wrapperWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At(value = "HEAD"), cancellable = true)
    private void initMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ModularWrapperWidget wrapperWidget) {
                    if (wrapperWidget.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void initKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ModularWrapperWidget wrapperWidget) {
                    if (wrapperWidget.keyPressed(keyCode, scanCode, modifiers)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void initKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ModularWrapperWidget wrapperWidget) {
                    if (wrapperWidget.keyPressed(keyCode, scanCode, modifiers)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

}
