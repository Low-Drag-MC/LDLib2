package com.lowdragmc.lowdraglib2.integration.rei;

import com.lowdragmc.lowdraglib2.gui.ui.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;

public final class ModularUIREIHandlers {
    public static final ExclusionZonesProvider<Screen> EXCLUSION_ZONES_PROVIDER = screen -> {
        var areas = new ArrayList<Rectangle>();
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder modularUIHolder) {
                for (var area : modularUIHolder.getModularUI().getGuiExtraAreas()) {
                    areas.add(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        }
        return areas;
    };

    @SuppressWarnings({"unchecked"})
    public static final FocusedStackProvider FOCUSED_STACK_PROVIDER = (screen, mouse) -> {
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder holder) {
                var lastHovered = holder.getModularUI().getLastHoveredElement();
                if (lastHovered == null) continue;
                var event = UIEvent.create(REIUIEvents.FOCUSED_STACK);
                event.target = lastHovered;
                event.x = mouse.getX();
                event.y = mouse.getY();
                UIEventDispatcher.dispatchEvent(event);
                if (event.customData instanceof CompoundEventResult<?> compoundEventResult && compoundEventResult.object() instanceof EntryStack<?>) {
                    return (CompoundEventResult<EntryStack<?>>) compoundEventResult;
                }
            }
        }
        return CompoundEventResult.pass();
    };
}
