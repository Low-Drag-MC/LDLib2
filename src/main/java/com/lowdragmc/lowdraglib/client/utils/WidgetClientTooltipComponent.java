package com.lowdragmc.lowdraglib.client.utils;

import com.lowdragmc.lowdraglib.gui.util.WidgetTooltipComponent;
import com.lowdragmc.lowdraglib.integration.jei.ModularWrapper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/6/29
 * @implNote WidgetClientTooltipComponent
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public record WidgetClientTooltipComponent(WidgetTooltipComponent tooltipComponent) implements ClientTooltipComponent {
    @Override
    public int getHeight() {
        return tooltipComponent.widget().getSize().height;
    }

    @Override
    public int getWidth(Font font) {
        return tooltipComponent.widget().getSize().width;
    }

    @Override
    public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
        var modularWrapper = new ModularWrapper<>(tooltipComponent.widget());
        modularWrapper.setRecipeWidget(x, y);
        modularWrapper.draw(graphics, 0, 0, Minecraft.getInstance().getTimer().getGameTimeDeltaTicks());
    }
}
