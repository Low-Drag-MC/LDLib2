//package com.lowdragmc.lowdraglib2.integration.xei.jei.handler;
//
//import mezz.jei.api.ingredients.ITypedIngredient;
//import mezz.jei.api.recipe.RecipeIngredientRole;
//import oshi.util.tuples.Pair;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public final class JEIRecipeIngredientHandler {
//    public final List<Pair<RecipeIngredientRole, List<ITypedIngredient<?>>>> focuses = new ArrayList<>();
//
//    public void add(RecipeIngredientRole role, List<ITypedIngredient<?>> typedIngredients) {
//        focuses.add(new Pair<>(role, typedIngredients));
//    }
//
//    public void add(RecipeIngredientRole role, ITypedIngredient<?>... typedIngredients) {
//        focuses.add(new Pair<>(role, Arrays.stream(typedIngredients).toList()));
//    }
//}
