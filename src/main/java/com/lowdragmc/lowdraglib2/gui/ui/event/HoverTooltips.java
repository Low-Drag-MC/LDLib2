package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@KJSBindings(clientOnly = true)
public record HoverTooltips(List<ClientTooltipComponent> tooltips,
                            @Nullable Font tooltipFont,
                            @Nullable ClientTooltipPositioner positioner,
                            @Nullable Identifier background,
                            @Nullable ItemStack tooltipStack) {

    public static HoverTooltips empty() {
        return new HoverTooltips(List.of(), null, null, null, null);
    }

    public static HoverTooltips create(Object... tooltips) {
        return new HoverTooltips(parseTooltips(tooltips), null, null, null, null);
    }

    private static List<ClientTooltipComponent> parseTooltips(Object... tooltips) {
        if (tooltips.length == 0) return Collections.emptyList();
        var newTooltips = new ArrayList<ClientTooltipComponent>();
        for (Object tooltip : tooltips) {
            if (tooltip == null) continue;
            switch (tooltip) {
                case ClientTooltipComponent clientTooltipComponent -> newTooltips.add(clientTooltipComponent);
                case TooltipComponent tooltipComponent ->
                        newTooltips.add(ClientTooltipComponent.create(tooltipComponent));
                case Component component ->
                        newTooltips.add(ClientTooltipComponent.create(component.getVisualOrderText()));
                case FormattedCharSequence formattedCharSequence ->
                        newTooltips.add(ClientTooltipComponent.create(formattedCharSequence));
                default ->
                        newTooltips.add(ClientTooltipComponent.create(Component.literal(tooltip.toString()).getVisualOrderText()));
            }
        }
        return newTooltips;
    }

    public HoverTooltips append(Object... tooltips) {
        var list = new ArrayList<>(this.tooltips);
        list.addAll(parseTooltips(tooltips));
        return new HoverTooltips(list, tooltipFont, positioner, background, tooltipStack);
    }

    public HoverTooltips tooltips(Object... tooltips) {
        return new HoverTooltips(parseTooltips(tooltips), tooltipFont, positioner, background, tooltipStack);
    }

    public HoverTooltips font(Font tooltipFont) {
        return new HoverTooltips(tooltips, tooltipFont, positioner, background, tooltipStack);
    }

    public HoverTooltips background(Identifier background) {
        return new HoverTooltips(tooltips, tooltipFont, positioner, background, tooltipStack);
    }

    public HoverTooltips stack(ItemStack tooltipStack) {
        return new HoverTooltips(tooltips, tooltipFont, positioner, background, tooltipStack);
    }

    public HoverTooltips positioner(ClientTooltipPositioner positioner) {
        return new HoverTooltips(tooltips, tooltipFont, positioner, background, tooltipStack);
    }
}
