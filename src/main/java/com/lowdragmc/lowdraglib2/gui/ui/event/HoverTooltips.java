package com.lowdragmc.lowdraglib2.gui.ui.event;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

@KJSBindings(clientOnly = true)
public record HoverTooltips(List<Component> tooltipTexts,
                            @Nullable TooltipComponent tooltipComponent,
                            @Nullable Font tooltipFont,
                            @Nullable ItemStack tooltipStack) {

    public static HoverTooltips empty() {
        return new HoverTooltips(List.of(), null, null, null);
    }

    public HoverTooltips append(Component... components) {
        var list = new ArrayList<>(tooltipTexts);
        list.addAll(List.of(components));
        return new HoverTooltips(list, tooltipComponent, tooltipFont, tooltipStack);
    }

    public HoverTooltips tooltipComponent(TooltipComponent tooltipComponent) {
        return new HoverTooltips(tooltipTexts, tooltipComponent, tooltipFont, tooltipStack);
    }

    public HoverTooltips font(Font tooltipFont) {
        return new HoverTooltips(tooltipTexts, tooltipComponent, tooltipFont, tooltipStack);
    }

    public HoverTooltips stack(ItemStack tooltipStack) {
        return new HoverTooltips(tooltipTexts, tooltipComponent, tooltipFont, tooltipStack);
    }
}
