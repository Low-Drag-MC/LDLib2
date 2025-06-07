package com.lowdragmc.lowdraglib2.integration.jei;

import com.lowdragmc.lowdraglib2.gui.ingredient.IRecipeIngredientSlot;

public interface IRecipeSlotBuilderAccessor {
    IRecipeIngredientSlot lowDragLib$getRecipeIngredientSlot();
    void lowDragLib$setRecipeIngredientSlot(IRecipeIngredientSlot recipeIngredientSlot);
}
