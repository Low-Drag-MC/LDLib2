package com.lowdragmc.lowdraglib2.integration.xei.jei.handler;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.xei.jei.JEIUIEvents;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class JEIRecipeSlotHandler {
    private static final String SLOT_PREFIX = "ldlib:recipe_slot/";
    private final Map<UIElement, MutableBinding> bindings = new LinkedHashMap<>();

    public static List<Binding> collectBindings(ModularUI modularUI) {
        var handler = new JEIRecipeSlotHandler();
        var event = UIEvent.create(JEIUIEvents.RECIPE_BINDING);
        event.target = modularUI.ui.rootElement;
        event.customData = handler;
        UIEventDispatcher.dispatchAllChildren(event);
        var pendingBindings = new ArrayList<>(handler.bindings.values()).reversed();
        var result = new ArrayList<Binding>(pendingBindings.size());
        for (var binding : pendingBindings) {
            result.add(binding.build(result.size()));
        }
        return List.copyOf(result);
    }

    public void addIngredients(UIElement element, RecipeIngredientRole role,
                               List<? extends ITypedIngredient<?>> ingredients) {
        var binding = bindings.computeIfAbsent(element, MutableBinding::new);
        binding.role = role;
        binding.ingredients = List.copyOf(ingredients);
        binding.hasAddedIngredients = true;
    }

    public void addSlot(UIElement element, RecipeIngredientRole fallbackRole,
                        List<? extends ITypedIngredient<?>> fallbackIngredients,
                        SlotUpdater slotUpdater) {
        var binding = bindings.computeIfAbsent(element, MutableBinding::new);
        if (!binding.hasAddedIngredients) {
            binding.role = fallbackRole;
            binding.ingredients = List.copyOf(fallbackIngredients);
        }
        binding.slotUpdater = slotUpdater;
    }

    public record Binding(
            int index,
            UIElement element,
            RecipeIngredientRole role,
            List<ITypedIngredient<?>> ingredients,
            @Nullable SlotUpdater slotUpdater
    ) {
        public String name() {
            return SLOT_PREFIX + index;
        }

        public boolean isInteractive() {
            return slotUpdater != null;
        }
    }

    public static final class SlotUpdater {
        private final Consumer<@Nullable ITypedIngredient<?>> displayedIngredientListener;
        @Nullable
        private ITypedIngredient<?> displayOverride;
        private boolean hasDisplayOverride;
        @Nullable
        private IRecipeSlotDrawable slot;

        public SlotUpdater(Consumer<@Nullable ITypedIngredient<?>> displayedIngredientListener) {
            this.displayedIngredientListener = displayedIngredientListener;
        }

        public void setDisplayedIngredient(@Nullable ITypedIngredient<?> displayedIngredient) {
            this.displayOverride = displayedIngredient;
            this.hasDisplayOverride = true;
            if (slot != null) {
                applyDisplayOverride(slot);
            }
        }

        public void bind(IRecipeSlotDrawable slot) {
            this.slot = slot;
            onDisplayedIngredientsUpdate();
        }

        public void onDisplayedIngredientsUpdate() {
            var slot = this.slot;
            if (slot == null) return;
            if (hasDisplayOverride) {
                applyDisplayOverride(slot);
            } else {
                displayedIngredientListener.accept(slot.getDisplayedIngredient().orElse(null));
            }
        }

        private void applyDisplayOverride(IRecipeSlotDrawable slot) {
            slot.clearDisplayOverrides();
            var overrides = slot.createDisplayOverrides();
            if (displayOverride != null) {
                overrides.addTypedIngredient(displayOverride);
            }
        }
    }

    private static final class MutableBinding {
        private final UIElement element;
        private RecipeIngredientRole role = RecipeIngredientRole.RENDER_ONLY;
        private List<ITypedIngredient<?>> ingredients = List.of();
        private boolean hasAddedIngredients;
        @Nullable
        private SlotUpdater slotUpdater;

        private MutableBinding(UIElement element) {
            this.element = element;
        }

        private Binding build(int index) {
            return new Binding(
                    index,
                    element,
                    role,
                    ingredients,
                    slotUpdater
            );
        }
    }
}
