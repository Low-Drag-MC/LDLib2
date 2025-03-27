package com.lowdragmc.lowdraglib.integration.jei;

import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;

public interface IRecipeSlotBuilderAccessor {
    IRecipeIngredientSlot lowDragLib$getRecipeIngredientSlot();
    void lowDragLib$setRecipeIngredientSlot(IRecipeIngredientSlot recipeIngredientSlot);
}
