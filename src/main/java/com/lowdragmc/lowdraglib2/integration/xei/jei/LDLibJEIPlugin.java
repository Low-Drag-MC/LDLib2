package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEIRecipeSlotHandler;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEITargetsTypedHandler;
import com.lowdragmc.lowdraglib2.test.xei.TestJEIPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@JeiPlugin
@ParametersAreNonnullByDefault
public class LDLibJEIPlugin implements IModPlugin {
    @Nullable
    public static IJeiRuntime jeiRuntime;
    @Nullable
    public static IIngredientManager ingredientManager;

    public static Rect2i getArea(UIElement element) {
        return getArea(element, false);
    }

    public static Rect2i getArea(UIElement element, boolean content) {
        Vector2f pos;
        Vector2f size;
        if (content) {
            pos = new Vector2f(element.getContentX(), element.getContentY());
            size = new Vector2f(element.getContentWidth(), element.getContentHeight());
        } else {
            pos = new Vector2f(element.getPositionX(), element.getPositionY());
            size = new Vector2f(element.getSizeWidth(), element.getSizeHeight());
        }
        var worldPos = element.localToWorld(pos);
        var worldSize = element.localToWorldNormal(size);
        return new Rect2i((int) worldPos.x, (int) worldPos.y, (int) worldSize.x, (int) worldSize.y);
    }

    public static Rect2i getAreaLocal(UIElement element, boolean content) {
        Vector2f pos;
        Vector2f size;
        if (content) {
            pos = new Vector2f(element.getContentX(), element.getContentY());
            size = new Vector2f(element.getContentWidth(), element.getContentHeight());
        } else {
            pos = new Vector2f(element.getPositionX(), element.getPositionY());
            size = new Vector2f(element.getSizeWidth(), element.getSizeHeight());
        }
        var worldPos = element.localToWorld(pos);
        var worldSize = element.localToWorldNormal(size);
        var mui = element.getModularUI();
        if (mui == null) return new Rect2i((int) worldPos.x, (int) worldPos.y, (int) worldSize.x, (int) worldSize.y);
        var localPos = mui.ui.rootElement.worldToLocalLayoutOffset(worldPos);
        var localSize = mui.ui.rootElement.worldToLocalNormal(worldSize);
        return new Rect2i((int) localPos.x, (int) localPos.y, (int) localSize.x, (int) localSize.y);
    }

    public static RecipeIngredientRole getRole(IngredientIO ingredientIO) {
        return switch (ingredientIO) {
            case INPUT -> RecipeIngredientRole.INPUT;
            case OUTPUT -> RecipeIngredientRole.OUTPUT;
            case CATALYST -> RecipeIngredientRole.CRAFTING_STATION;
            default -> RecipeIngredientRole.RENDER_ONLY;
        };
    }

    public static <V> Optional<ITypedIngredient<V>> createTypedIngredient(IIngredientType<V> ingredientType, V ingredient) {
        if (ingredientManager == null) return Optional.empty();
        return ingredientManager.createTypedIngredient(ingredientType, ingredient, false);
    }

    @Override
    public Identifier getPluginUid() {
        return LDLib2.id("jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        LDLibJEIPlugin.jeiRuntime = jeiRuntime;
        LDLibJEIPlugin.ingredientManager = jeiRuntime.getIngredientManager();
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
        ingredientManager = null;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(ModularUIScreen.class, ModularUIJEIHandlers.GHOST_INGREDIENT_HANDLER);
        registration.addGhostIngredientHandler(AbstractContainerScreen.class, ModularUIJEIHandlers.GHOST_INGREDIENT_HANDLER);
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, ModularUIJEIHandlers.GUI_CONTAINER_HANDLER);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // Cache the IngredientManager as early as possible. registerCategories runs to
        // completion across all plugins before any plugin's registerRecipes, so other mods
        // that call into LDLib2 helpers from their registerRecipes will find it ready.
        ingredientManager = registration.getJeiHelpers().getIngredientManager();
        if (Platform.isDevEnv()) {
            TestJEIPlugin.registerCategories(registration);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Platform.isDevEnv()) {
            TestJEIPlugin.registerRecipes(registration);
        }
    }

    /// Utilities for xei compat
    /**
     * Adds a clickable ingredient functionality to a specified UI element for use with JEI integration.
     * The provided {@code clickableBuilder} defines how the clickable ingredient will be created
     * based on the UI element.
     * <br>
     * For example, if you want to lookup the jei recipe while clicking the element of your ui.
     *
     * @param <T> The type of the {@code UIElement} to which the clickable ingredient will be added.
     * @param <I> The type of the ingredient handled by {@code ITypedIngredient}.
     * @param element The UI element to which the clickable ingredient behavior will be bound.
     * @param clickableBuilder A function that provides an {@code ITypedIngredient} of current mouse.
     */
    public static <T extends UIElement, I> void clickableIngredient(T element, Supplier<@Nullable ITypedIngredient<I>> clickableBuilder) {
        element.addEventListener(JEIUIEvents.CLICKABLE_INGREDIENT, event -> {
            if (element.isMouseOverElement(event.x, event.y) && event.customData instanceof IClickableIngredientFactory factory) {
                var clickable = clickableBuilder.get();
                if (clickable == null) return;
                event.customData = factory.createBuilder(clickable).buildWithArea(LDLibJEIPlugin.getArea(element));
                event.stopPropagation();
            }
        });
    }

    /**
     * Adds ghost ingredient functionality to a specified {@code UIElement}.
     * The ghost ingredient can appear as a visual placeholder on the element,
     * based on the conditions defined by the provided {@code Predicate}.
     * <br>
     * For example, if you want your element to accept dragging ingredients from JEI.
     *
     * @param <T> The type of the {@link UIElement} to which the functionality is being added.
     * @param <I> The type of the ingredient handled by {@link ITypedIngredient}.
     * @param element The {@link UIElement} where the ghost ingredient functionality is applied.
     * @param type The {@link IIngredientType} of the ingredient being handled.
     * @param mayPlace A {@link Predicate} that determines whether the ghost ingredient can be placed.
     * @param onPlace A {@link Consumer} to handle the action of placing the ghost ingredient.
     */
    public static <T extends UIElement, I> void ghostIngredient(T element, IIngredientType<I> type,
                                                                Predicate<ITypedIngredient<I>> mayPlace,
                                                                Consumer<I> onPlace) {
        element.addEventListener(JEIUIEvents.GHOST_INGREDIENT, event -> {
            if (event.customData instanceof JEITargetsTypedHandler<?> targets) {
                Optional.ofNullable(targets.ingredient.cast(type)).ifPresent(typedIngredient -> {
                    if (mayPlace.test(typedIngredient)) {
                        targets.add(LDLibJEIPlugin.getArea(element, true), onPlace);
                    }
                });
            }
        });
    }

    /**
     * Adds recipe ingredient functionality to a specified {@link UIElement}.
     * This event to provide ingredients of the specific ingredient role to the recipe.
     * <br>
     * For example, if you want to add input/output/catalyst ingredients to the recipe based on the UI element.
     *
     * @param <T> The type of the {@code UIElement} to which the functionality is being added.
     * @param element The {@link UIElement} to which the recipe ingredient behavior will be bound.
     * @param ingredientIO The {@link IngredientIO} specifying the type of ingredient input/output.
     * @param ingredientsProvider A {@link Supplier} that provides a list of {@link ITypedIngredient} values to associate with the element.
     */
    public static <T extends UIElement> void recipeIngredient(T element, IngredientIO ingredientIO,
                                                              Supplier<? extends List<? extends ITypedIngredient<?>>> ingredientsProvider) {
        element.addEventListener(JEIUIEvents.RECIPE_BINDING, event -> {
            if (event.customData instanceof JEIRecipeSlotHandler handler) {
                handler.addIngredients(element, getRole(ingredientIO), ingredientsProvider.get());
            }
        });
    }

    /**
     * Binds one LDLib element to one genuine JEI recipe slot.
     * The complete static alternatives are snapshotted when JEI lays out the recipe, while
     * the returned listener keeps the genuine slot synchronized with LDLib's displayed ingredient.
     * <p>
     * The element's additional tooltips are added through the genuine slot's rich-tooltip callback,
     * so JEI displays them alongside its standard ingredient tooltip.
     *
     * @param <T> The type of the {@link UIElement} to bind.
     * @param <V> The ingredient value type.
     * @param element The element whose position, hover behavior, and additional tooltips represent the slot.
     * @param ingredientIO The semantic role of the slot in the recipe.
     * @param ingredientType The JEI ingredient type for the slot's values.
     * @param ingredientsProvider The complete static alternatives to snapshot during recipe layout.
     * @param displayedIngredientListener A listener that updates LDLib's externally rendered value when JEI
     *                                    cycles the genuine slot.
     * @return A listener that updates the genuine JEI slot when LDLib's displayed ingredient changes.
     */
    public static <T extends UIElement, V> Consumer<V> recipeSlot(
            T element,
            IngredientIO ingredientIO,
            IIngredientType<V> ingredientType,
            Supplier<Stream<V>> ingredientsProvider,
            Consumer<@Nullable V> displayedIngredientListener
    ) {
        var slotUpdater = new JEIRecipeSlotHandler.SlotUpdater(displayedIngredient ->
                displayedIngredientListener.accept(displayedIngredient == null ? null :
                        displayedIngredient.getCastIngredient(ingredientType)));
        element.addEventListener(JEIUIEvents.RECIPE_BINDING, event -> {
            if (event.customData instanceof JEIRecipeSlotHandler handler) {
                var ingredients = ingredientsProvider.get()
                        .map(ingredient -> createTypedIngredient(ingredientType, ingredient))
                        .flatMap(Optional::stream)
                        .toList();
                handler.addSlot(element, getRole(ingredientIO), ingredients, slotUpdater);
            }
        });
        return ingredient -> slotUpdater.setDisplayedIngredient(
                createTypedIngredient(ingredientType, ingredient).orElse(null));
    }

}
