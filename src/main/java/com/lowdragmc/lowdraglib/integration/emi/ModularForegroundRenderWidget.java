package com.lowdragmc.lowdraglib.integration.emi;

import com.lowdragmc.lowdraglib.integration.jei.ModularWrapper;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Optional;

public class ModularForegroundRenderWidget extends Widget {
    public final ModularWrapper<?> modular;

    public ModularForegroundRenderWidget(ModularWrapper<?> modular) {
        this.modular = modular;
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(0, 0, modular.getWidget().getSize().width, modular.getWidget().getSize().height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(-modular.getLeft(), -modular.getTop(), 0);


        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        modular.modularUI.mainGroup.drawInForeground(graphics, mouseX, mouseY, partialTick);
        modular.modularUI.mainGroup.drawOverlay(graphics, mouseX, mouseY, partialTick);

        // do not draw tooltips here, do it from recipe viewer.
        if (modular.isShouldRenderTooltips() && modular.tooltipTexts != null && !modular.tooltipTexts.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 240);
            graphics.renderTooltip(Minecraft.getInstance().font, modular.tooltipTexts, Optional.ofNullable(modular.tooltipComponent), mouseX, mouseY);
            graphics.pose().popPose();
            graphics.bufferSource().endLastBatch();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        graphics.pose().popPose();
    }
}
