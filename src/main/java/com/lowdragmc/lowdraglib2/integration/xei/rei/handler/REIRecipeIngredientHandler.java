package com.lowdragmc.lowdraglib2.integration.xei.rei.handler;

import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import java.util.ArrayList;
import java.util.List;

public final class REIRecipeIngredientHandler {
    public final List<EntryIngredient> inputs = new ArrayList<>();
    public final List<EntryIngredient> outputs = new ArrayList<>();

    public void addInput(List<EntryIngredient> inputIngredients) {
        inputs.addAll(inputIngredients);
    }

    public void addOutput(List<EntryIngredient> outputIngredients) {
        outputs.addAll(outputIngredients);
    }

    public void add(IngredientIO role, List<EntryIngredient> ingredients) {
        if (role == IngredientIO.INPUT) {
            addInput(ingredients);
        } else if (role == IngredientIO.OUTPUT) {
            addOutput(ingredients);
        }
    }
}
