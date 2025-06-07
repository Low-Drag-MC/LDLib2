package com.lowdragmc.lowdraglib2.integration.jei;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lowdragmc.lowdraglib2.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author KilaBash
 * @date 2022/04/30
 * @implNote ModularUIRecipeCategory
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ModularUIRecipeCategory<T> implements IRecipeCategory<T> {
    private final LoadingCache<T, ModularWrapper<?>> modularWrapperCache;

    protected ModularUIRecipeCategory(Function<T, ModularWrapper<?>> wrapperFunction) {
        this.modularWrapperCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .maximumSize(10)
                .build(new CacheLoader<>() {
                    @Override
                    public ModularWrapper<?> load(T key) {
                        return wrapperFunction.apply(key);
                    }
                });
    }

    private ModularWrapper<?> getModularWrapper(T recipe) {
        return this.modularWrapperCache.getUnchecked(recipe);
    }

    private static void addJEISlot(IRecipeLayoutBuilder builder, IRecipeIngredientSlot slot, RecipeIngredientRole role, int index) {
        var slotName = "slot_" + index;
        var slotBuilder = builder.addSlot(role, slot.self().getPositionX(), slot.self().getPositionY());
        if (slotBuilder instanceof IRecipeSlotBuilderAccessor accessor) {
            accessor.lowDragLib$setRecipeIngredientSlot(slot);
        }
        // append ingredients
        var ingredientMap = new HashMap<IIngredientType, List>();
        for (Object ingredient : slot.getXEIIngredients()) {
            if (ingredient instanceof IClickableIngredient clickableIngredient) {
                ingredientMap.computeIfAbsent(clickableIngredient.getIngredientType(), k -> new ArrayList())
                        .add(clickableIngredient.getIngredient());
            }
        }
        for (var entry : ingredientMap.entrySet()) {
            var type = entry.getKey();
            var ingredients = entry.getValue();
            slotBuilder.addIngredients(type, ingredients);
            slotBuilder.setCustomRenderer(type, new IIngredientRenderer<>() {
                @Override
                public void render(GuiGraphics guiGraphics, Object ingredient) {
                }

                @Override
                public List<Component> getTooltip(Object ingredient, TooltipFlag tooltipFlag) {
                    return Collections.emptyList();
                }

                @Override
                public void getTooltip(ITooltipBuilder tooltip, Object ingredient, TooltipFlag tooltipFlag) {
                    tooltip.addAll(slot.getFullTooltipTexts());
                }

                @Override
                public int getWidth() {
                    return slot.self().getSizeWidth();
                }

                @Override
                public int getHeight() {
                    return slot.self().getSizeHeight();
                }
            });
        }
        // set slot name
        slotBuilder.setSlotName(slotName);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        var wrapper = getModularWrapper(recipe);

        wrapper.setRecipeWidget(0, 0);
        List<Widget> flatVisibleWidgetCollection = wrapper.modularUI.getFlatWidgetCollection();
        for (int i = 0; i < flatVisibleWidgetCollection.size(); i++) {
            var widget = flatVisibleWidgetCollection.get(i);
            if (widget instanceof IRecipeIngredientSlot slot) {
                var role = mapToRole(slot.getIngredientIO());
                if (role == null) { // both
                    addJEISlot(builder, slot, RecipeIngredientRole.INPUT, i);
                    addJEISlot(builder, slot, RecipeIngredientRole.OUTPUT, i);
                } else {
                    addJEISlot(builder, slot, role, i);
                }
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
        var wrapper = getModularWrapper(recipe);

        builder.addGuiEventListener(new ModularUIGuiEventListener<>(wrapper));
        builder.addWidget(new ModularForegroundRecipeWidget(wrapper));
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var wrapper = getModularWrapper(recipe);

        wrapper.draw(guiGraphics, (int) mouseX, (int) mouseY, Minecraft.getInstance().getTimer().getGameTimeDeltaTicks());
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        IRecipeCategory.super.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);

        var wrapper = getModularWrapper(recipe);
        if (wrapper.tooltipTexts != null && !wrapper.tooltipTexts.isEmpty()) {
            tooltip.addAll(wrapper.tooltipTexts);
        }
        if (wrapper.tooltipComponent != null) {
            tooltip.add(wrapper.tooltipComponent);
        }
    }

    @Nullable
    public static RecipeIngredientRole mapToRole(IngredientIO ingredientIO) {
        return switch (ingredientIO) {
            case INPUT -> RecipeIngredientRole.INPUT;
            case OUTPUT -> RecipeIngredientRole.OUTPUT;
            case CATALYST -> RecipeIngredientRole.CATALYST;
            case RENDER_ONLY -> RecipeIngredientRole.RENDER_ONLY;
            case BOTH -> null;
        };
    }

}
