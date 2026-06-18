package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IModularUIProvider;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEIRecipeIngredientHandler;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEIRecipeWidgetHandler;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ModularUIRecipeCategory<T> implements IRecipeCategory<T> {
    public static final String SLOT_PREFIX = "ldlib:recipe_slot/";
    public final IModularUIProvider<T> uiProvider;
    // runtime
    protected final LoadingCache<T, ModularUI> uiCache;

    protected ModularUIRecipeCategory(IModularUIProvider<T> provider) {
        this.uiProvider = provider;
        this.uiCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .maximumSize(10)
                .removalListener((RemovalNotification<T, ModularUI> notification) -> {
                    var value = notification.getValue();
                    if (value != null) {
                        value.onRemoved();
                    }
                })
                .build(new CacheLoader<>() {
                    @Override
                    public ModularUI load(T key) {
                        var mui = uiProvider.createModularUI(key);
                        mui.setAllowDebugMode(false);
                        mui.setDrawTooltips(false);
                        mui.init(getWidth(), getHeight());
                        return mui;
                    }
                });
    }

    @Override
    public abstract int getWidth();

    @Override
    public abstract int getHeight();

    public ModularUI getUIForRecipe(T recipe) {
        return uiCache.getUnchecked(recipe);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        // post event to append ingredient focus
        var ingredientFocus = new JEIRecipeIngredientHandler();
        var mui = getUIForRecipe(recipe);
        var event = UIEvent.create(JEIUIEvents.RECIPE_INGREDIENT);
        event.target = mui.ui.rootElement;
        event.customData = ingredientFocus;
        UIEventDispatcher.dispatchAllChildren(event);
        var ingredientIndex = 0;
        // reversed to keep correct order
        for (var focus : ingredientFocus.focuses.reversed()) {
//            builder.addInvisibleIngredients(focus.getA()).addTypedIngredients(focus.getB());
            var area = focus.area();
            var slotBuilder = builder.addSlot(focus.role())
                    .addTypedIngredients(focus.ingredients())
                    .setSlotName(SLOT_PREFIX + ingredientIndex)
                    .setPosition(area.getX(), area.getY());
            for (ITypedIngredient<?> ingredient : focus.ingredients()) {
                var type = ingredient.getType();
                slotBuilder.setCustomRenderer(type, new IIngredientRenderer() {
                    @Override
                    public void render(GuiGraphics guiGraphics, Object ingredient) {}

                    @Override
                    public List<Component> getTooltip(Object ingredient, TooltipFlag tooltipFlag) {
                        return List.of();
                    }

                    @Override
                    public int getWidth() {
                        return area.getWidth();
                    }

                    @Override
                    public int getHeight() {
                        return area.getHeight();
                    }
                });
            }
            ingredientIndex++;
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
        IRecipeCategory.super.createRecipeExtras(builder, recipe, focuses);
        var mui = getUIForRecipe(recipe);
        var widget = new ModularUIJEIWidget(getUIForRecipe(recipe));
        builder.addWidget(widget);
        builder.addGuiEventListener(widget);

        // post event to append recipe slots
        var recipeSlot = new JEIRecipeWidgetHandler(widget::getLocalToWorld);
        var event = UIEvent.create(JEIUIEvents.RECIPE_WIDGET);
        event.target = widget.modularUI.ui.rootElement;
        event.customData = recipeSlot;
        UIEventDispatcher.dispatchAllChildren(event);

        for (var slot : recipeSlot.slots) {
            builder.addSlottedWidget(new ISlottedRecipeWidget() {
                @Override
                public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
                    return Optional.ofNullable(slot.getRecipeSlots(mouseX, mouseY));
                }

                @Override
                public ScreenPosition getPosition() {
                    return ModularUIJEIWidget.ZERO;
                }
            }, List.of());
        }

        var ingredientFocus = new JEIRecipeIngredientHandler();
        event = UIEvent.create(JEIUIEvents.RECIPE_INGREDIENT);
        event.target = mui.ui.rootElement;
        event.customData = ingredientFocus;
        UIEventDispatcher.dispatchAllChildren(event);
        var ingredientIndex = 0;
        // let's hide all recipe slots
        while (true) {
            var found = builder.getRecipeSlots().findSlotByName(SLOT_PREFIX + ingredientIndex);
            if (found.isEmpty()) break;
            found.ifPresent(slot -> {
                builder.addSlottedWidget(new ISlottedRecipeWidget() {
                    @Override
                    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
                        return Optional.empty();
                    }

                    @Override
                    public ScreenPosition getPosition() {
                        return ModularUIJEIWidget.ZERO;
                    }
                }, List.of(slot));
            });
            ingredientIndex++;
        }
    }
}
