package com.lowdragmc.lowdraglib2.integration.xei;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import org.jetbrains.annotations.Nullable;

/**
 * Handed to the listeners of {@link UIEvents#HOVER_TOOLTIPS} through {@link UIEvent#customData} when the
 * tooltips are collected for an XEI (JEI/REI/EMI) recipe viewer instead of the modular ui itself.
 */
public enum XEITooltipContext {
    /**
     * The tooltips are collected for a recipe slot. The recipe viewer renders the tooltip of the
     * ingredient on its own, so listeners should drop whatever would duplicate it, e.g. the display
     * name of a fluid or the vanilla tooltip of an item.
     */
    RECIPE_SLOT;

    /**
     * Collects the tooltips of the given element for this context.
     * <p>
     * Resolves them the same way {@link UIElement#collectHoverTooltips()} does, the only difference is
     * that this context is handed to the listeners through {@link UIEvent#customData}.
     *
     * @return the tooltips of the element, or {@code null} if it provides none.
     */
    @Nullable
    public HoverTooltips collectTooltips(UIElement element) {
        var event = UIEvent.create(UIEvents.HOVER_TOOLTIPS);
        event.hasBubblePhase = false;
        event.hasCapturePhase = false;
        event.target = element;
        event.customData = this;
        UIEventDispatcher.dispatchDirectEvent(event, false);
        if (event.hoverTooltips != null) {
            return event.hoverTooltips;
        }
        var styleTooltips = element.getStyle().tooltips();
        if (!styleTooltips.isEmpty()) {
            return HoverTooltips.create((Object[]) styleTooltips.tooltips());
        }
        return null;
    }
}
