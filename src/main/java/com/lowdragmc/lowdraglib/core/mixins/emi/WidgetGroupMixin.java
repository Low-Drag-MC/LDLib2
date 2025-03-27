package com.lowdragmc.lowdraglib.core.mixins.emi;

import com.lowdragmc.lowdraglib.integration.emi.ModularWrapperWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * @author KilaBash
 * @date 2023/4/3
 * @implNote WidgetGroupMixin
 */
@Mixin(WidgetGroup.class)
public abstract class WidgetGroupMixin {

    @Shadow(remap = false) @Final public int x;

    @Shadow(remap = false) @Final public int y;

    @Inject(method = "add", at = @At(value = "RETURN"), remap = false)
    private <T extends Widget> void init(T widget, CallbackInfoReturnable<T> cir) {
        if (widget instanceof ModularWrapperWidget wrapperWidget) {
            wrapperWidget.modular.setEmiRecipeWidget(this.x, this.y);
        }
    }
}
