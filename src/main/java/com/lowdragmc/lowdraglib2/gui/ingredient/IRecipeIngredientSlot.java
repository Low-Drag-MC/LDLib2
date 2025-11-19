package com.lowdragmc.lowdraglib2.gui.ingredient;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public interface IRecipeIngredientSlot extends IIngredientSlot {

    default UIElement self() {
        return (UIElement) this;
    }

    /**
     * Used for focus ingredient under the mouse
     */
    @Nullable
    @Override
    default Object getXEIIngredientOverMouse(double mouseX, double mouseY) {
        if (self().isMouseOverElement(mouseX, mouseY)) {
            var ingredients = getXEIIngredients();
            if (!ingredients.isEmpty()) {
                return ingredients.getFirst();
            }
        }
        return null;
    }

    /**
     * All ingredients that may be displayed
     */
    List<Object> getXEIIngredients();

    /**
     * Current ingredient that is displayed
     */
    @Nullable
    default Object getXEICurrentIngredient() {
        var ingredients = getXEIIngredients();
        if (ingredients.isEmpty()) {
            return null;
        }
        return ingredients.getFirst();
    }

    default float getXEIChance() {
        return 1.0f;
    }

//    default IngredientIO getIngredientIO(){
//        return IngredientIO.RENDER_ONLY;
//    }

    /**
     * Get full tooltips excluding the ingredient's tooltip.
     */
    default List<Component> getFullTooltipTexts() {
        return Collections.emptyList();
    }

}
