package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record ModularUIClientElementComponent(ModularUITooltipComponent modularUITooltipComponent) implements ClientTooltipComponent {
    @Override
    public int getHeight(Font font) {
        return (int) modularUITooltipComponent.modularUI.getHeight();
    }

    @Override
    public int getWidth(Font font) {
        return (int) modularUITooltipComponent.modularUI.getWidth();
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        ModularUIClientAccess.getWidget(modularUITooltipComponent.modularUI)
                .extractRenderState(graphics, 0, 0, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        graphics.pose().popMatrix();
    }
}
