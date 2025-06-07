package com.lowdragmc.lowdraglib2.integration.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;

import java.util.Optional;

public class ModularForegroundRecipeWidget implements IRecipeWidget {
    public final ModularWrapper<?> modular;

    public ModularForegroundRecipeWidget(ModularWrapper<?> modular) {
        this.modular = modular;
    }

    @Override
    public ScreenPosition getPosition() {
        return new ScreenPosition(0, 0);
    }

    @Override
    public void draw(GuiGraphics graphics, double mouseX, double mouseY) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();

        graphics.pose().pushPose();
        graphics.pose().translate(-modular.getLeft(), -modular.getTop(), 0);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        modular.modularUI.mainGroup.drawInForeground(graphics, (int) mouseX, (int) mouseY, partialTick);
        modular.modularUI.mainGroup.drawOverlay(graphics, (int) mouseX, (int) mouseY, partialTick);

        // do not draw tooltips here, do it from recipe viewer.
        if (modular.isShouldRenderTooltips() && modular.tooltipTexts != null && !modular.tooltipTexts.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 240);
            graphics.renderTooltip(Minecraft.getInstance().font, modular.tooltipTexts, Optional.ofNullable(modular.tooltipComponent), (int) mouseX, (int) mouseY);
            graphics.pose().popPose();
            graphics.bufferSource().endLastBatch();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        graphics.pose().popPose();
    }
}
