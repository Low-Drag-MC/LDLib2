package com.lowdragmc.lowdraglib2.test;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class TestEmiRecipeHandler implements StandardRecipeHandler<AbstractContainerMenu> {
    @Override
    public List<Slot> getInputSources(AbstractContainerMenu handler) {
        return handler.slots;
    }

    @Override
    public List<Slot> getCraftingSlots(AbstractContainerMenu handler) {
        return handler.slots;
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof TestEMIPlugin.TestEmiRecipe;
    }
}
