package com.lowdragmc.lowdraglib2.integration.xei.rei;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.experimental.UtilityClass;

@KJSBindings(modId = "rei")
@UtilityClass
public final class REIUIEvents {
    public static final String FOCUSED_STACK = "focusedStack";
    public static final String DRAGGABLE_STACK_BOUNDS = "draggableStackBounds";
    public static final String ACCEPT_DRAGGABLE_STACK = "acceptDraggableStack";
    public static final String RECIPE_INGREDIENT = "recipeIngredient";
    public static final String RECIPE_WIDGET = "recipeWidget";
}
