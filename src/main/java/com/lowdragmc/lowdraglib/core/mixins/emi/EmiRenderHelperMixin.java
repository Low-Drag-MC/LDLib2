package com.lowdragmc.lowdraglib.core.mixins.emi;

import com.lowdragmc.lowdraglib.integration.emi.ModularEmiRecipe;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.WidgetHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


/**
 * @author KilaBash
 * @date 2023/4/3
 * @implNote EmiRenderHelperMixin
 */
@Mixin(EmiRenderHelper.class)
public abstract class EmiRenderHelperMixin {
    @Redirect(method = "renderRecipe", at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/recipe/EmiRecipe;addWidgets(Ldev/emi/emi/api/widget/WidgetHolder;)V"), remap = false)
    private static void injectRenderRecipe(EmiRecipe instance, WidgetHolder widgetHolder) {
        if (instance instanceof ModularEmiRecipe<?> modularEmiRecipe) {
            modularEmiRecipe.addTempWidgets(widgetHolder);
        } else {
            instance.addWidgets(widgetHolder);
        }
    }
}
