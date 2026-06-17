package com.lowdragmc.lowdraglib2.integration.xei.jei.handler;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class JEIRecipeIngredientHandler {
    public record Entry(RecipeIngredientRole role, List<ITypedIngredient<?>> ingredients, Rect2i area) {

    }

    public final List<Entry> focuses = new ArrayList<>();

    public void add(Entry entry) {
        focuses.add(entry);
    }

    public void add(RecipeIngredientRole role, List<ITypedIngredient<?>> typedIngredients) {
        focuses.add(new Entry(role, typedIngredients, new Rect2i(0, 0, 16, 16)));
    }

    public void add(RecipeIngredientRole role, ITypedIngredient<?>... typedIngredients) {
        focuses.add(new Entry(role, Arrays.stream(typedIngredients).toList(), new Rect2i(0, 0, 16, 16)));
    }
}
