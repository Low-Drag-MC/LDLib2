package com.lowdragmc.lowdraglib.core.mixins.jei;

import com.lowdragmc.lowdraglib.jei.IRecipeSlotBuilderAccessor;
import com.lowdragmc.lowdraglib.jei.IRecipeIngredientSlotWrapper;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.library.gui.recipes.layout.builder.RecipeSlotBuilder;
import mezz.jei.library.ingredients.DisplayIngredientAcceptor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RecipeSlotBuilder.class)
public class RecipeSlotBuilderMixin implements IRecipeSlotBuilderAccessor {
    @Shadow @Final private DisplayIngredientAcceptor ingredients;
    @Shadow private ImmutableRect2i rect;
    @Shadow private @Nullable IDrawable overlay;
    @Unique
    public com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot lowDragLib$recipeIngredientSlot;

    @Override
    public com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot lowDragLib$getRecipeIngredientSlot() {
        return lowDragLib$recipeIngredientSlot;
    }

    @Override
    public void lowDragLib$setRecipeIngredientSlot(com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot recipeIngredientSlot) {
        this.lowDragLib$recipeIngredientSlot = recipeIngredientSlot;
        this.rect = new ImmutableRect2i(
                this.rect.getX(),
                this.rect.getY(),
                recipeIngredientSlot.self().getSizeWidth(),
                recipeIngredientSlot.self().getSizeHeight()
        );
        this.overlay = new IRecipeIngredientSlotWrapper(recipeIngredientSlot);
    }

}