package com.lowdragmc.lowdraglib2.integration.emi;

import com.lowdragmc.lowdraglib2.gui.ui.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.jei.JEIUIEvents;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import lombok.experimental.UtilityClass;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;
import java.util.function.Consumer;

@UtilityClass
public final class ModularUIEMIHandlers {
    public final static EmiExclusionArea<Screen> EXCLUSION_AREA = (Screen screen, Consumer<Bounds> consumer) -> {
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder modularUIHolder) {
                for (var area : modularUIHolder.getModularUI().getGuiExtraAreas()) {
                    consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        }
    };

    public final static EmiStackProvider<Screen> STACK_PROVIDER = (screen, x, y) -> {
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder modularUIHolder) {
                var lastHovered = modularUIHolder.getModularUI().getLastHoveredElement();
                if (lastHovered == null) continue;
                var event = UIEvent.create(EMIUIEvents.STACK_PROVIDER);
                event.target = lastHovered;
                event.x = x;
                event.y = y;
                UIEventDispatcher.dispatchEvent(event);
                if (event.customData instanceof EmiStackInteraction interaction) {
                    return interaction;
                }
            }
        }
        return EmiStackInteraction.EMPTY;
    };
}
