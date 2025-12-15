package com.lowdragmc.lowdraglib2.integration.xei.jei.handler;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class JEITargetsTypedHandler<I> {
    public final boolean doStart;
    public final ITypedIngredient<I> ingredient;
    public final List<IGhostIngredientHandler.Target<I>> targets;

    public JEITargetsTypedHandler(boolean doStart, ITypedIngredient<I> ingredient) {
        this.doStart = doStart;
        this.ingredient = ingredient;
        this.targets = new ArrayList<>();
    }

    public void add(IGhostIngredientHandler.Target<I> target) {
        targets.add(target);
    }

    public <T> void add(Rect2i area, Consumer<T> onClicked) {
        add(new IGhostIngredientHandler.Target<>() {
            @Override
            public Rect2i getArea() {
                return area;
            }

            @Override
            public void accept(I ingredient) {
                onClicked.accept((T) ingredient);
            }
        });
    }

}
