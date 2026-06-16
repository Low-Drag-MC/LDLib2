package com.lowdragmc.lowdraglib2.integration.xei.jei.handler;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class JEIRecipeSlotHandler {
    private final List<SlotEntry> slots = new ArrayList<>();

    public void addSlot(SlotEntry slot) {
        slots.add(slot);
    }

    public void registerLayoutSlots(IRecipeLayoutBuilder builder) {
        for (var slot : slots) {
            if (slot.role() == RecipeIngredientRole.RENDER_ONLY) {
                continue;
            }
            var ingredients = slot.ingredients();
            if (ingredients.isEmpty()) {
                continue;
            }
            var slotBuilder = builder.addSlot(slot.role())
                    .setPosition(slot.x(), slot.y())
                    .addTypedIngredients(ingredients);
            if (slot.tooltipCallback() != null) {
                slotBuilder.addRichTooltipCallback(slot.tooltipCallback());
            }
            slotBuilder.setSlotName(slot.slotName());
        }
    }

    /**
     * Resolves the {@link IRecipeSlotDrawable}s that JEI created in {@code setRecipe} for the given slot name.
     * Slots are registered with a stable name in {@link #registerLayoutSlots}, so a name match is enough -
     * no need to rebuild the entry or fall back to ingredient comparison.
     */
    public static List<IRecipeSlotDrawable> findByName(IRecipeSlotDrawablesView view, String slotName) {
        return view.getSlots().stream()
                .filter(slot -> slot.getSlotName().map(slotName::equals).orElse(false))
                .toList();
    }

    public record SlotEntry(
            RecipeIngredientRole role,
            int x,
            int y,
            Supplier<@Nullable ITypedIngredient<?>> displayedIngredient,
            @Nullable Supplier<List<@Nullable ITypedIngredient<?>>> allIngredients,
            @Nullable IRecipeSlotRichTooltipCallback tooltipCallback,
            String slotName
    ) {
        public List<ITypedIngredient<?>> ingredients() {
            if (allIngredients != null) {
                var ingredients = allIngredients.get().stream()
                        .filter(Objects::nonNull)
                        .toList();
                if (!ingredients.isEmpty()) {
                    return ingredients;
                }
            }
            var displayed = displayedIngredient.get();
            return displayed == null ? List.of() : List.of(displayed);
        }
    }
}
