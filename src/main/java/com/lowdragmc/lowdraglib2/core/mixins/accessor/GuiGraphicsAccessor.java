package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Reaches the tooltip renderer that takes both a {@link ClientTooltipPositioner} and a
 * {@code TooltipComponent}. Every public overload has one or the other, never both.
 *
 * <p>{@code tooltipStack} comes with it because {@code renderTooltipInternal} reads the field for
 * NeoForge's tooltip events.
 */
@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {

    @Invoker("renderTooltipInternal")
    void ldlib2$renderTooltipInternal(Font font, List<ClientTooltipComponent> components,
                                      int mouseX, int mouseY, ClientTooltipPositioner positioner);

    @Accessor("tooltipStack")
    void ldlib2$setTooltipStack(ItemStack stack);
}
