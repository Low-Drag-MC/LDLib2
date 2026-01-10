package com.lowdragmc.lowdraglib2.integration.xei.jei.handler;

import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class JEIRecipeWidgetHandler {
    public final List<RecipeSlotProvider> slots = new ArrayList<>();
    public final Supplier<Matrix4f> localToWorld;

    public JEIRecipeWidgetHandler(Supplier<Matrix4f> localToWorld) {
        this.localToWorld = localToWorld;
    }

    public void addSlot(RecipeSlotProvider slotUnderMouse) {
        slots.add(slotUnderMouse);
    }

    public void addSlot(IRecipeSlotDrawable slot) {
        addSlot((mouseX, mouseY) -> {
            if (slot.isMouseOver(mouseX, mouseY)) {
                return new RecipeSlotUnderMouse(slot, 0, 0);
            }
            return null;
        });
    }

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
