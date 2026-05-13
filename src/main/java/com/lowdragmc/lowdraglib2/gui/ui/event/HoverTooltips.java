package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
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
public record HoverTooltips(List<Object> tooltips,
                            @Nullable Object tooltipFont,
                            @Nullable Object positioner,
                            @Nullable Identifier background,
                            @Nullable ItemStack tooltipStack) {

    public static HoverTooltips empty() {
        return new HoverTooltips(List.of(), null, null, null, null);
    }

    public static HoverTooltips create(Object... tooltips) {
        return new HoverTooltips(parseTooltips(tooltips), null, null, null, null);
    }

    private static List<Object> parseTooltips(Object... tooltips) {
        if (tooltips.length == 0) return Collections.emptyList();
        var newTooltips = new ArrayList<>();
        for (Object tooltip : tooltips) {
            if (tooltip == null) continue;
            if (tooltip instanceof Component || tooltip instanceof FormattedCharSequence || tooltip instanceof ClientTooltipComponent || tooltip instanceof TooltipComponent) {
                newTooltips.add(tooltip);
            } else {
                newTooltips.add(Component.literal(tooltip.toString()));
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

    public HoverTooltips font(Object tooltipFont) {
        return new HoverTooltips(tooltips, tooltipFont, positioner, background, tooltipStack);
    }

    public HoverTooltips background(Identifier background) {
        return new HoverTooltips(tooltips, tooltipFont, positioner, background, tooltipStack);
    }

    public HoverTooltips stack(ItemStack tooltipStack) {
        return new HoverTooltips(tooltips, tooltipFont, positioner, background, tooltipStack);
    }

    public HoverTooltips positioner(Object positioner) {
        return new HoverTooltips(tooltips, tooltipFont, positioner, background, tooltipStack);
    }
}
