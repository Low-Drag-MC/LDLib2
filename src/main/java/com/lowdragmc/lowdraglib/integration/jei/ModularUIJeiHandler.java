package com.lowdragmc.lowdraglib.integration.jei;

import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModularUIJeiHandler implements IGuiContainerHandler<ModularUIGuiContainer>, IGhostIngredientHandler<ModularUIGuiContainer>{

    @Override
    @Nonnull
    public List<Rect2i> getGuiExtraAreas(@Nonnull ModularUIGuiContainer containerScreen) {
        return containerScreen.getGuiExtraAreas();
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(ModularUIGuiContainer gui, double mouseX, double mouseY) {
        if (gui.modularUI.mainGroup.getXEIIngredientOverMouse(mouseX, mouseY) instanceof IClickableIngredient<?> clickableIngredient) {
            return Optional.of(clickableIngredient);
        }
        return Optional.empty();
    }

    @NotNull
    @Override
    public <I> List<Target<I>> getTargetsTyped(ModularUIGuiContainer gui, @NotNull ITypedIngredient<I> ingredient, boolean doStart) {
        List<com.lowdragmc.lowdraglib.gui.ingredient.Target> targets = gui.modularUI.mainGroup.getPhantomTargets(ingredient);
        if (targets.isEmpty()) return Collections.emptyList();
        return targets.stream().map(target-> new JEITarget<I>(target)).collect(Collectors.toList());
    }

    @Override
    public void onComplete() {
    }

    public static class JEITarget<I> implements Target<I> {
        com.lowdragmc.lowdraglib.gui.ingredient.Target target;

        public JEITarget(com.lowdragmc.lowdraglib.gui.ingredient.Target target) {
            this.target = target;
        }

        @Override
        public Rect2i getArea() {
            return target.getArea();
        }

        @Override
        public void accept(I ingredient) {
            target.accept(ingredient);
        }

    }
}
