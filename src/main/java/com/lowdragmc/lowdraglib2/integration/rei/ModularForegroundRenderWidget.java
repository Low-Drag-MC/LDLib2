package com.lowdragmc.lowdraglib2.integration.rei;

import com.lowdragmc.lowdraglib2.integration.jei.ModularWrapper;
import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ModularForegroundRenderWidget extends Widget {
    public final ModularWrapper<?> modular;

    public ModularForegroundRenderWidget(ModularWrapper<?> modular) {
        this.modular = modular;
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

        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        graphics.pose().popPose();

    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }
}
