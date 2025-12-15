package com.lowdragmc.lowdraglib2.integration.xei.emi;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.experimental.UtilityClass;

@KJSBindings(modId = "emi")
@UtilityClass
public final class EMIUIEvents {
    // TODO Documents necessary
    public static final String STACK_PROVIDER = "stackProvider";
    public static final String RENDER_DRAG_HANDLER = "renderDragHandler";
    public static final String DROP_STACK_HANDLER = "dropStackHandler";
    public static final String RECIPE_INGREDIENT = "recipeIngredient";
    public static final String RECIPE_WIDGET = "recipeWidget";
}
