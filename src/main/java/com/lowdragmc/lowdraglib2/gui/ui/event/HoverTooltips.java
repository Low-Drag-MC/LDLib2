package com.lowdragmc.lowdraglib2.gui.ui.event;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public record HoverTooltips(List<Component> tooltipTexts,
                            @Nullable TooltipComponent tooltipComponent,
                            @Nullable Font tooltipFont,
                            @Nullable ItemStack tooltipStack) {

}
