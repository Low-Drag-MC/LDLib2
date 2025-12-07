package com.lowdragmc.lowdraglib2.integration.xei.emi.handler;

import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

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
            addInput(ingredients);
        } else if (role == IngredientIO.CATALYST) {
            addCatalyst(ingredients);
        } else if (role == IngredientIO.OUTPUT) {
            addOutput(ingredients.stream().flatMap(ingredient -> ingredient.getEmiStacks().stream()).toList());
        }
    }
}
