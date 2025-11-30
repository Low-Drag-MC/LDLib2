package com.lowdragmc.lowdraglib2.integration.jei;

import com.lowdragmc.lowdraglib2.gui.ui.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIJEIHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {
    @Override
    public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen) {
        var areas = new ArrayList<Rect2i>();
        for (GuiEventListener child : containerScreen.children()) {
            if (child instanceof IModularUIHolder modularUIHolder) {
                areas.addAll(modularUIHolder.getModularUI().getGuiExtraAreas());
            }
        }
        return areas;
    }

    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory builder, AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
        for (GuiEventListener child : containerScreen.children()) {
            if (child instanceof IModularUIHolder modularUIHolder) {
                var lastHovered = modularUIHolder.getModularUI().getLastHoveredElement();
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
}