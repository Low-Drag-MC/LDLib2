package com.lowdragmc.lowdraglib2.gui.ui.utils;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public record ModularUIClientElementComponent(ModularUITooltipComponent modularUITooltipComponent) implements ClientTooltipComponent {
    @Override
    public int getHeight() {
        return (int) modularUITooltipComponent.modularUI.getHeight();
    }

    @Override
    public int getWidth(Font font) {
        return (int) modularUITooltipComponent.modularUI.getWidth();
    }

    @Override
    public void renderImage(Font textRenderer, int x, int y, GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        modularUITooltipComponent.modularUI.getWidget()
                .render(graphics, 0, 0, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        graphics.pose().popPose();
    }
}
