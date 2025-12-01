package com.lowdragmc.lowdraglib2.integration.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record JEITarget<T>(Rect2i area, Consumer<T> onClicked) implements IGhostIngredientHandler.Target<T> {
    @Override
    public Rect2i getArea() {
        return area;
    }

    @Override
    public void accept(T ingredient) {
        onClicked.accept(ingredient);
    }
}
