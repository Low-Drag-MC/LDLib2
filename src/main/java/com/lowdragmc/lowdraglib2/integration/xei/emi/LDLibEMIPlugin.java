package com.lowdragmc.lowdraglib2.integration.xei.emi;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIDragDropHandler;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIRecipeIngredientHandler;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIRecipeWidgetHandler;
import com.lowdragmc.lowdraglib2.test.xei.TestEMIPlugin;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import org.joml.Vector2f;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@EmiEntrypoint
public class LDLibEMIPlugin implements EmiPlugin {

    public static Bounds getBounds(UIElement element) {
        return getBounds(element, false);
    }

    public static Bounds getBounds(UIElement element, boolean content) {
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
        return new Bounds((int) worldPos.x, (int) worldPos.y, (int) worldSize.x, (int) worldSize.y);
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea(ModularUIEMIHandlers.EXCLUSION_AREA);
        registry.addGenericStackProvider(ModularUIEMIHandlers.STACK_PROVIDER);
        registry.addGenericDragDropHandler(ModularUIEMIHandlers.DRAG_DROP_HANDLER);
        if (Platform.isDevEnv()) {
            TestEMIPlugin.register(registry);
        }
    }


    /// Utilities for xei compat
    /**
     * Adds a clickable ingredient functionality to a specified UI element for use with EMI integration.
     * <br>
     * For example, if you want to lookup the emi recipe while clicking the element of your ui.
     *
     * @param <T>        the type of the UI element, constrained to {@link UIElement}.
     * @param element    the UI element to which the stack provider callback is attached.
     * @param interaction a supplier returning {@link EmiStackInteraction}, providing the
     *                    custom interaction logic for the event.
     */
    public static <T extends UIElement> void stackProvider(T element, Supplier<EmiStackInteraction> interaction) {
        element.addEventListener(EMIUIEvents.STACK_PROVIDER, event -> {
           if (element.isMouseOverElement(event.x, event.y)) {
               event.customData = interaction.get();
               event.stopPropagation();
           }
        });
    }

    /**
     * Configures a drag handler for a given UI element to allow interaction with draggable ingredients
     * in the EMI integration. The handler checks whether the dragged ingredient can be placed in the
     * bounds of the specified UI element.
     *
     * @param <T>        the type of the UI element, constrained to {@link UIElement}.
     * @param element    the UI element to which the drag handler will be attached.
     * @param canPlace   a predicate that tests if a dragged {@link EmiIngredient} can be placed
     *                   within the designated UI element.
     */
    public static <T extends UIElement> void renderDragHandler(T element, Predicate<EmiIngredient> canPlace) {
        element.addEventListener(EMIUIEvents.RENDER_DRAG_HANDLER, event -> {
            if (event.customData instanceof EMIDragDropHandler handlers) {
                if (canPlace.test(handlers.dragged)) {
                    handlers.bounds.add(LDLibEMIPlugin.getBounds(element, true));
                }
            }
        });
    }

    /**
     * Attaches a drop stack handler to a specified {@link UIElement}. This handler enables interaction
     * for dropping {@link EmiIngredient} objects into the designated UI element.
     *
     * @param <T>       the type of the UI element, constrained to {@link UIElement}.
     * @param element   the UI element to which the drop handler will be attached.
     * @param canPlace  a predicate to test whether the dropped {@link EmiIngredient} can be placed
     *                  into the specified UI element.
     * @param onPlace   a consumer that specifies the action to be performed when an {@link EmiIngredient}
     *                  is successfully placed in the UI element.
     */
    public static <T extends UIElement> void dropStackHandler(T element, Predicate<EmiIngredient> canPlace, Consumer<EmiIngredient> onPlace) {
        element.addEventListener(EMIUIEvents.DROP_STACK_HANDLER, event -> {
            if (event.customData instanceof EmiIngredient dragged && element.isMouseOverElement(event.x, event.y)) {
                if (canPlace.test(dragged)) {
                    onPlace.accept(dragged);
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
     * @param <T>                the type of the UI element, constrained to {@link UIElement}.
     * @param element            the UI element to which the event listener will be attached.
     * @param role               the {@link IngredientIO} role to be associated with the ingredients.
     * @param ingredientsProvider a supplier providing a list of {@link EmiIngredient} objects to be added.
     */
    public static <T extends UIElement> void recipeIngredient(T element, IngredientIO role, Supplier<List<EmiIngredient>> ingredientsProvider) {
        element.addEventListener(EMIUIEvents.RECIPE_INGREDIENT, event -> {
            if (event.customData instanceof EMIRecipeIngredientHandler recipeIngredient) {
                recipeIngredient.add(role, ingredientsProvider.get());
            }
        });
    }

    /**
     * Adds recipe slot(widget) functionality to the EMI recipe.
     * This allows associating an invisible slot in the UI element for EMI lookups and tooltips.
     *
     * @param <T> The type of the {@link UIElement} to which the slot functionality is added.
     * @param element The {@link UIElement} where the recipe slot functionality is applied.
     * @param displayIngredient A {@link Supplier} that provides the primary {@link EmiIngredient}
     *                          to be displayed in the slot.
     */
    public static <T extends UIElement> void recipeSlot(T element,
                                                        Supplier<EmiIngredient> displayIngredient) {
        element.addEventListener(EMIUIEvents.RECIPE_WIDGET, event -> {
            if (event.customData instanceof EMIRecipeWidgetHandler recipeSlot) {
                var slot = new EMIRecipeSlotWidget(displayIngredient,
                        recipeSlot.localToWorld,
                        element::isMouseOverElement,
                        () -> getBounds(element));
                for (var component : element.getStyle().tooltips().asList()) {
                    slot.appendTooltip(component);
                }
                recipeSlot.addWidget(slot);
            }
        });
        // we block all mouse down events to prevent skipping emi event
        element.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            e.hasHandler = false;
            e.stopImmediatePropagation();
        });
    }
}
