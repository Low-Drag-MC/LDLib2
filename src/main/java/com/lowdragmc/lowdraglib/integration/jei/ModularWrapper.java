package com.lowdragmc.lowdraglib.integration.jei;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.math.Position;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class ModularWrapper<T extends Widget> extends ModularUIGuiContainer {
    @Getter
    protected T widget;
    @Getter @Setter
    protected boolean shouldRenderTooltips = false;

    public ModularWrapper(T widget) {
        super(new ModularUI(widget.getSize().width, widget.getSize().height, IUIHolder.EMPTY, Minecraft.getInstance().player).widget(widget), -1);
        modularUI.initWidgets();
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        this.widget = widget;
    }

    private int lastTick;
    @Getter
    private int left, top;

    public String getUid() {
        return null;
    }

    /**
     * For REI to use
     */
    public void setRecipeWidget(int left, int top) {
        modularUI.initWidgets();
        this.left = 0;
        this.top = 0;
        this.width = minecraft.getWindow().getGuiScaledWidth();
        this.height = minecraft.getWindow().getGuiScaledHeight();
        modularUI.updateScreenSize(this.width, this.height);
        Position displayOffset = Position.of(left, top);
        modularUI.mainGroup.setParentPosition(displayOffset);
    }

    public void setEmiRecipeWidget(int left, int top) {
        modularUI.initWidgets();
        this.left = left;
        this.top = top;
        this.width = minecraft.getWindow().getGuiScaledWidth();
        this.height = minecraft.getWindow().getGuiScaledHeight();
        modularUI.updateScreenSize(this.width, this.height);
        Position displayOffset = Position.of(left, top);
        modularUI.mainGroup.setParentPosition(displayOffset);
    }

    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (minecraft.player.tickCount != lastTick) {
            updateScreen();
            lastTick = minecraft.player.tickCount;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(-left, -top, 0);
        render(graphics, mouseX + left, mouseY + top, partialTicks);
        graphics.pose().popPose();
    }

    public void updateScreen() {
        modularUI.mainGroup.updateScreen();
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.hoveredSlot = null;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        tooltipTexts = null;
        tooltipFont = null;
        tooltipStack = ItemStack.EMPTY;
        tooltipComponent = null;

        modularUI.mainGroup.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        modularUI.mainGroup.drawInForeground(graphics, mouseX, mouseY, partialTicks);

        // do not draw tooltips here, do it from recipe viewer.
        if (shouldRenderTooltips && tooltipTexts != null && !tooltipTexts.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 240);
            graphics.renderTooltip(font, tooltipTexts, Optional.ofNullable(tooltipComponent), mouseX, mouseY);
            graphics.pose().popPose();
            graphics.bufferSource().endLastBatch();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

}
