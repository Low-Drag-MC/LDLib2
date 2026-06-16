package com.lowdragmc.lowdraglib2.integration.xei.jei.handler;

import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class JEIRecipeWidgetHandler {
    public final List<SlotWidgetEntry> slots = new ArrayList<>();
    public final Supplier<Matrix3x2f> localToWorld;
    public final Supplier<IRecipeSlotDrawablesView> recipeSlots;

    public JEIRecipeWidgetHandler(Supplier<Matrix3x2f> localToWorld) {
        this(localToWorld, () -> List::of);
    }

    public JEIRecipeWidgetHandler(Supplier<Matrix3x2f> localToWorld, Supplier<IRecipeSlotDrawablesView> recipeSlots) {
        this.localToWorld = localToWorld;
        this.recipeSlots = recipeSlots;
    }

    public void addSlot(RecipeSlotProvider slotUnderMouse) {
        addSlot(slotUnderMouse, List::of);
    }

    public void addSlot(RecipeSlotProvider slotUnderMouse, Supplier<List<IRecipeSlotDrawable>> handledSlots) {
        slots.add(new SlotWidgetEntry(slotUnderMouse, handledSlots));
    }

    public void addSlot(IRecipeSlotDrawable slot) {
        addSlot((mouseX, mouseY) -> {
            if (slot.isMouseOver(mouseX, mouseY)) {
                return new RecipeSlotUnderMouse(slot, 0, 0);
            }
            return null;
        });
    }

    public record SlotWidgetEntry(RecipeSlotProvider provider, Supplier<List<IRecipeSlotDrawable>> handledSlots) {}

    @FunctionalInterface
    public interface RecipeSlotProvider extends BiFunction<Double, Double, RecipeSlotUnderMouse> {
        @Nullable
        RecipeSlotUnderMouse getRecipeSlots(double mouseX, double mouseY);

        @Override
        @Nullable
        default RecipeSlotUnderMouse apply(Double mouseX, Double mouseY) {
            return getRecipeSlots(mouseX, mouseY);
        }
    }
}
