package com.lowdragmc.lowdraglib2.integration.xei.rei;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.rei.handler.REIDraggableStackBoundsHandler;
import com.lowdragmc.lowdraglib2.integration.xei.rei.handler.REIRecipeIngredientHandler;
import com.lowdragmc.lowdraglib2.integration.xei.rei.handler.REIRecipeWidgetHandler;
import com.lowdragmc.lowdraglib2.test.xei.TestREIPlugin;
import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@REIPluginClient
public class
LDLibREIPlugin implements REIClientPlugin {

    public static Rectangle getRectangle(UIElement element) {
        return getRectangle(element, false);
    }

    public static Rectangle getRectangle(UIElement element, boolean content) {
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
        return new Rectangle((int) worldPos.x, (int) worldPos.y, (int) worldSize.x, (int) worldSize.y);
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(Screen.class, ModularUIREIHandlers.EXCLUSION_ZONES_PROVIDER);
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerFocusedStack(ModularUIREIHandlers.FOCUSED_STACK_PROVIDER);
        registry.registerDraggableStackVisitor(ModularUIREIHandlers.DRAGGABLE_STACK_VISITOR);
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        if (Platform.isDevEnv()) {
            TestREIPlugin.registerCategories(registry);
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (Platform.isDevEnv()) {
            TestREIPlugin.registerDisplays(registry);
        }
    }

    /// Utilities for xei compat
    /**
     * Adds a focus stack functionality to the specified {@link UIElement},
     * allowing it to respond to mouse interaction events and provide custom
     * data based on the supplied focus stack provider.
     * <br>
     * For example, if you want to lookup the rei recipe while clicking the element of your ui.
     *
     * @param <T> The type of the {@link UIElement} implementing the functionality.
     * @param <I> The type of the data object supplied by the {@code focusedStackProvider}.
     * @param element The {@link UIElement} to which the focus stack functionality is applied.
     * @param focusedStackProvider A {@link Supplier} that provides the data to be set as focus
     *                             when interaction with the {@code UIElement} occurs.
     */
    public static <T extends UIElement, I> void focusedStack(T element, Supplier<I> focusedStackProvider) {
        element.addEventListener(REIUIEvents.FOCUSED_STACK, event -> {
            if (element.isMouseOverElement(event.x, event.y)) {
                var value = focusedStackProvider.get();
                if (value == null) return;
                event.customData = CompoundEventResult.interruptTrue(value);
                event.stopPropagation();
            }
        });
    }

    /**
     * Adds draggable stack bounds to a specified {@code UIElement}.
     * The dragging ingredient can appear as a visual placeholder on the element,
     * based on the conditions defined by the provided {@code Predicate}.
     * <br>
     * For example, if you want your element to accept dragging ingredients from REI.
     *
     * @param <T> The type of the {@link UIElement} to which the functionality is being added.
     * @param <I> The type of the stack type handled by {@link EntryType}.
     * @param element The {@link UIElement} where the ghost ingredient functionality is applied.
     * @param type The {@link EntryStack} of the ingredient being handled.
     * @param mayPlace A {@link Predicate} that determines whether the ghost ingredient can be placed.
     */
    public static <T extends UIElement, I> void draggableStackBounds(T element, EntryType<I> type,
                                                                     Predicate<EntryStack<I>> mayPlace) {
        element.addEventListener(REIUIEvents.DRAGGABLE_STACK_BOUNDS, event -> {
            if (event.customData instanceof REIDraggableStackBoundsHandler handler) {
                var target = handler.stack.get();
                if (target == type && mayPlace.test(target.cast())) {
                    handler.boundsProviders.add(DraggableStackVisitor.BoundsProvider.ofRectangle(LDLibREIPlugin.getRectangle(element, true)));
                }
            }
        });
    }


    /**
     * Accept draggable stack to a specified {@link UIElement}. This handler enables interaction
     * for dropping {@link EntryStack} objects into the designated UI element.
     *
     * @param <T>       the type of the UI element, constrained to {@link UIElement}.
     * @param element   the UI element to which the drop handler will be attached.
     * @param mayPlace  a predicate to test whether the dropped {@link EntryStack} can be placed
     *                  into the specified UI element.
     * @param onPlace   a consumer that specifies the action to be performed when an {@link EntryStack}
     *                  is successfully placed in the UI element.
     */
    public static <T extends UIElement, I> void acceptDraggableStack(T element, EntryType<I> type,
                                                              Predicate<EntryStack<I>> mayPlace,
                                                              Consumer<EntryStack<I>> onPlace) {
        element.addEventListener(REIUIEvents.ACCEPT_DRAGGABLE_STACK, event -> {
            if (event.customData instanceof REIDraggableStackBoundsHandler handler &&
                    handler.context.getCurrentPosition() instanceof Point point &&
                    element.isMouseOverElement(point.x, point.y)) {
                var target = handler.stack.get();
                if (target == type && mayPlace.test(target.cast())) {
                    onPlace.accept(target.cast());
                    event.stopPropagation();
                }
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
     * @param ingredientsProvider A {@link Supplier} that provides a list of {@link EntryIngredient} values to associate with the element.
     */
    public static <T extends UIElement> void recipeIngredient(T element, IngredientIO ingredientIO,
                                                              Supplier<List<EntryIngredient>> ingredientsProvider) {
        element.addEventListener(REIUIEvents.RECIPE_INGREDIENT, event -> {
            if (event.customData instanceof REIRecipeIngredientHandler recipeIngredient) {
                recipeIngredient.add(ingredientIO, ingredientsProvider.get());
            }
        });
    }

    /**
     * Adds recipe widgets functionality to the REI recipe.
     * This allows associating an invisible slot in the UI element for REI lookups and tooltips.
     *
     * @param <T>                The type of the {@link UIElement} to which the functionality is being added.
     * @param element            The {@link UIElement} to associate with the recipe slot functionality.
     * @param ingredientIO       The {@link IngredientIO} type that determines whether the element is used for input or output.
     * @param displayedIngredient A {@link Supplier} providing the primary {@link EntryStack} to be displayed in the recipe slot.
     * @param allIngredients     An optional {@link Supplier} providing a list of all possible {@link EntryStack}s available for the recipe slot.
     */
    public static <T extends UIElement> void recipeSlot(T element, IngredientIO ingredientIO,
                                                        Supplier<EntryStack<?>> displayedIngredient,
                                                        @Nullable Supplier<List<EntryStack<?>>> allIngredients) {
        element.addEventListener(REIUIEvents.RECIPE_WIDGET, event -> {
            if (event.customData instanceof REIRecipeWidgetHandler handler) {
                var recipeSlot = new REIRecipeSlotWidget(
                        handler.containerBounds,
                        handler.localToWorld,
                        element::isMouseOverElement,
                        () -> getRectangle(element),
                        displayedIngredient,
                        allIngredients,
                        (tooltip) -> tooltip.addAllTexts(element.getStyle().tooltips().asList()));
                if (ingredientIO == IngredientIO.INPUT) {
                    recipeSlot.markInput();
                } else if (ingredientIO == IngredientIO.OUTPUT) {
                    recipeSlot.markOutput();
                }
                handler.addWidget(recipeSlot);
            }
        });
    }
}
