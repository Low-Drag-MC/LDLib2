package com.lowdragmc.lowdraglib2.integration.xei.emi.handler;

import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;

import java.util.ArrayList;
import java.util.List;

public final class EMIRecipeIngredientHandler {
    public final List<EmiIngredient> inputs = new ArrayList<>();
    public final List<EmiIngredient> catalysts = new ArrayList<>();
    public final List<EmiStack> outputs = new ArrayList<>();

    public void addInput(List<EmiIngredient> inputIngredients) {
        inputs.addAll(inputIngredients);
    }

    public void addCatalyst(List<EmiIngredient> catalystIngredients) {
        catalysts.addAll(catalystIngredients);
    }

    public void addOutput(List<EmiStack> outputStacks) {
        outputs.addAll(outputStacks);
    }

    public void add(IngredientIO role, List<EmiIngredient> ingredients) {
        if (role == IngredientIO.INPUT) {
            addInput(mergeVariants(ingredients));
        } else if (role == IngredientIO.CATALYST) {
            addCatalyst(mergeVariants(ingredients));
        } else if (role == IngredientIO.OUTPUT) {
            addOutput(ingredients.stream().flatMap(ingredient -> ingredient.getEmiStacks().stream()).toList());
        }
    }

    private static List<EmiIngredient> mergeVariants(List<EmiIngredient> ingredients) {
        if (ingredients.size() < 2) return ingredients;
        return List.of(new ListEmiIngredient(ingredients, ingredients.getFirst().getAmount()));
    }
}
