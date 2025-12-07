package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEITargetsTypedHandler;
import lombok.experimental.UtilityClass;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@UtilityClass
public final class ModularUIJEIHandlers {
    public static final IGuiContainerHandler<AbstractContainerScreen<?>> GUI_CONTAINER_HANDLER = new IGuiContainerHandler<>() {
        @Override
        public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen) {
            var areas = new ArrayList<Rect2i>();
            for (var child : containerScreen.children()) {
                if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                    areas.addAll(holder.getModularUI().getGuiExtraAreas());
                }
            }
            return areas;
        }

        @Override
        public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory builder, AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
            for (var child : containerScreen.children()) {
                if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                    var lastHovered = holder.getModularUI().getLastHoveredElement();
                    if (lastHovered == null) continue;
                    var event = UIEvent.create(JEIUIEvents.CLICKABLE_INGREDIENT);
                    event.target = lastHovered;
                    event.x = (float) mouseX;
                    event.y = (float) mouseY;
                    event.customData = builder;
                    UIEventDispatcher.dispatchEvent(event);
                    if (event.customData instanceof Optional<?> clickableIngredient) {
                        if (clickableIngredient.isEmpty()) return Optional.empty();
                        if (clickableIngredient.get() instanceof IClickableIngredient<?> ci) return Optional.of(ci);
                    }
                }
            }
            return IGuiContainerHandler.super.getClickableIngredientUnderMouse(builder, containerScreen, mouseX, mouseY);
        }
    };


    public static final IGhostIngredientHandler GHOST_INGREDIENT_HANDLER = new IGhostIngredientHandler<>() {
        @Override
        public <I> List<Target<I>> getTargetsTyped(Screen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            var targets = new JEITargetsTypedHandler<>(doStart, ingredient);
            for (var child : gui.children()) {
                if (child instanceof IModularUIHolder holder && holder.getModularUI() != null) {
                    var mui = holder.getModularUI();
                    UIEvent event = UIEvent.create(JEIUIEvents.GHOST_INGREDIENT);
                    event.target = mui.ui.rootElement;
                    event.customData = targets;
                    UIEventDispatcher.dispatchAllChildren(event);
                }
            }
            return targets.targets;
        }

        @Override
        public void onComplete() {

        }
    };
}