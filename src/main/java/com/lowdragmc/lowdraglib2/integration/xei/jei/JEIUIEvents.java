package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.experimental.UtilityClass;

@KJSBindings(modId = "jei")
@UtilityClass
public final class JEIUIEvents {
    // TODO Documents necessary
    public static final String CLICKABLE_INGREDIENT = "clickableIngredient";
    public static final String GHOST_INGREDIENT = "ghostIngredient";
    public static final String RECIPE_INGREDIENT = "recipeIngredient";
    public static final String RECIPE_WIDGET = "recipeWidget";
}
