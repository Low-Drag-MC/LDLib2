package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IModularUIProvider;
import com.lowdragmc.lowdraglib2.integration.xei.XEITooltipContext;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEIRecipeSlotHandler;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.navigation.ScreenPosition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ModularUIRecipeCategory<T> implements IRecipeCategory<T> {
    public final IModularUIProvider<T> uiProvider;
    // runtime
    private final CachedRecipeUI<T> cachedRecipeUI;

    protected ModularUIRecipeCategory(IModularUIProvider<T> provider) {
        this.uiProvider = provider;
        this.cachedRecipeUI = new CachedRecipeUI<>(provider, this::getWidth, this::getHeight);
    }

    @Override
    public abstract int getWidth();

    @Override
    public abstract int getHeight();

    public ModularUI getUIForRecipe(T recipe) {
        return cachedRecipeUI.get(recipe).modularUI();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        for (var binding : cachedRecipeUI.get(recipe).bindings()) {
            var area = LDLibJEIPlugin.getAreaLocal(binding.element(), true);
            var tagLines = JEITagNameTooltip.resolve(binding);
            builder.addSlot(binding.role())
                    .addTypedIngredients(binding.ingredients())
                    .setSlotName(binding.name())
                    .setPosition(area.getX(), area.getY())
                    .addRichTooltipCallback((slot, tooltip) -> {
                        JEITagNameTooltip.append(binding, tagLines, slot, tooltip);
                        appendAdditionalTooltip(binding, tooltip);
                    });
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
        var recipeUI = cachedRecipeUI.get(recipe);
        var mui = recipeUI.modularUI();
        var widget = new ModularUIJEIWidget(mui);
        builder.addWidget(widget);
        builder.addGuiEventListener(widget);

        var bindings = recipeUI.bindings();
        var slottedBindings = new ArrayList<SlottedBinding>(bindings.size());
        for (var binding : bindings) {
            builder.getRecipeSlots().findSlotByName(binding.name())
                    .ifPresent(slot -> slottedBindings.add(new SlottedBinding(binding, slot)));
        }

        if (!slottedBindings.isEmpty()) {
            List<IRecipeSlotDrawable> slots = slottedBindings.stream().map(slottedBinding -> slottedBinding.slot).toList();
            builder.addSlottedWidget(new ModularUIRecipeWidget(slottedBindings, widget), slots);
        }
    }

    @Override
    public void onDisplayedIngredientsUpdate(T recipe, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
        for (var binding : cachedRecipeUI.get(recipe).bindings()) {
            var slotUpdater = binding.slotUpdater();
            if (slotUpdater != null) {
                slotUpdater.onDisplayedIngredientsUpdate();
            }
        }
    }

    private static void appendAdditionalTooltip(JEIRecipeSlotHandler.Binding binding, ITooltipBuilder tooltip) {
        var hoverTooltips = XEITooltipContext.RECIPE_SLOT.collectTooltips(binding.element());
        if (hoverTooltips == null) return;
        tooltip.addAll(hoverTooltips.tooltipTexts());
        if (hoverTooltips.tooltipComponent() != null) {
            tooltip.add(hoverTooltips.tooltipComponent());
        }
    }

    private static final class SlottedBinding {
        private final JEIRecipeSlotHandler.Binding binding;
        private final IRecipeSlotDrawable slot;

        private SlottedBinding(JEIRecipeSlotHandler.Binding binding, IRecipeSlotDrawable slot) {
            this.binding = binding;
            this.slot = slot;
            var slotUpdater = binding.slotUpdater();
            if (slotUpdater != null) {
                slotUpdater.bind(slot);
            }
        }
    }

    private static class ModularUIRecipeWidget implements ISlottedRecipeWidget {
        private final List<SlottedBinding> slottedBindings;
        private final ModularUIJEIWidget widget;

        public ModularUIRecipeWidget(List<SlottedBinding> slottedBindings, ModularUIJEIWidget widget) {
            this.slottedBindings = slottedBindings;
            this.widget = widget;
        }

        @Override
        public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
            var localMouse = widget.getWorldMouse((float) mouseX, (float) mouseY);
            for (var slottedBinding : slottedBindings) {
                var binding = slottedBinding.binding;
                if (binding.isInteractive() && binding.element().isMouseOverElement(localMouse.x, localMouse.y)) {
                    return Optional.of(new RecipeSlotUnderMouse(slottedBinding.slot, 0, 0));
                }
            }
            return Optional.empty();
        }

        @Override
        public ScreenPosition getPosition() {
            return ModularUIJEIWidget.ZERO;
        }
    }
}
