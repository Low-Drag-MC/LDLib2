package com.lowdragmc.lowdraglib2.integration.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;

import java.util.List;

public record JEITargetsTyped<I>(ITypedIngredient<I> ingredient, List<IGhostIngredientHandler.Target<I>> targets) {
}
