package com.lowdragmc.lowdraglib.core.mixins.jei;

import com.lowdragmc.lowdraglib.integration.jei.ClickableIngredient;
import com.lowdragmc.lowdraglib.integration.jei.IRecipeIngredientSlotWrapper;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.library.gui.ingredients.RecipeSlot;
import mezz.jei.library.gui.ingredients.RendererOverrides;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(RecipeSlot.class)
public abstract class RecipeSlotMixin {

    @Shadow @Final private @Nullable IDrawable overlay;

    @Mutable
    @Shadow @Final private @Nullable RendererOverrides rendererOverrides;

    @Inject(method = "getDisplayedIngredient", at = @At("RETURN"), cancellable = true, remap = false)
    private void lowDragLib$getDisplayedIngredient(CallbackInfoReturnable<Optional<ITypedIngredient<?>>> cir) {
        // I dont want to do this. but the JEI API changed frequently and I have to use a stable injection point.
        if (this.overlay instanceof IRecipeIngredientSlotWrapper lowDragLib$recipeIngredientSlot) {
            var currentIngredient = lowDragLib$recipeIngredientSlot.slot.getXEICurrentIngredient();
            ITypedIngredient<?> ingredientType = null;
            if (currentIngredient instanceof ITypedIngredient<?> typedIngredient) {
                ingredientType = typedIngredient;
            } else if (currentIngredient instanceof ClickableIngredient<?> clickableIngredient) {
                ingredientType = clickableIngredient.getTypedIngredient();
            }
            cir.setReturnValue(Optional.ofNullable(ingredientType));
        }
    }

    @Inject(method = "drawIngredient", at = @At("HEAD"), cancellable = true, remap = false)
    private void lowDragLib$drawIngredient(CallbackInfo ci) {
        if (this.overlay instanceof IRecipeIngredientSlotWrapper lowDragLib$recipeIngredientSlot) {
            ci.cancel();
        }
    }
}