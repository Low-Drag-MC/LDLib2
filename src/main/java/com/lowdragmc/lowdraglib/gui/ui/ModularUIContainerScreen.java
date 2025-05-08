package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ModularUIContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public final ModularUI modularUI;
    // hover tips
    @Nullable
    public List<Component> tooltipTexts;
    @Nullable
    public TooltipComponent tooltipComponent;
    @Nullable
    public Font tooltipFont;
    @Nullable
    public ItemStack tooltipStack = ItemStack.EMPTY;
    // drag element
    protected Tuple<Object, IGuiTexture> draggingElement;

    public ModularUIContainerScreen(ModularUI modularUI, T container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.modularUI = modularUI;
        modularUI.setScreen(this);
        this.imageWidth = (int) modularUI.rootElement.getSizeWidth();
        this.imageHeight = (int) modularUI.rootElement.getSizeHeight();
    }

    public boolean setDraggingElement(Object element, IGuiTexture renderer) {
        if (draggingElement != null) return false;
        draggingElement = new Tuple<>(element, renderer);
        return true;
    }

    @Nullable
    public Object getDraggingElement() {
        if (draggingElement == null) return null;
        return draggingElement.getA();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        modularUI.tick();
    }

    @Override
    public void init() {
        this.modularUI.init(width, height);
        this.imageWidth = (int) modularUI.rootElement.getSizeWidth();
        this.imageHeight = (int) modularUI.rootElement.getSizeHeight();
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.hoveredSlot = null;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        tooltipTexts = null;
        tooltipComponent = null;

        this.renderBackground(graphics, mouseX, mouseY, partialTicks);

        RenderSystem.depthMask(true);
        NeoForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(this, graphics));
        RenderSystem.depthMask(false);

        modularUI.rootElement.drawInBackground(graphics, mouseX, mouseY, partialTicks);

        RenderSystem.depthMask(true);
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Background(this, graphics, mouseX, mouseY));
        RenderSystem.depthMask(false);

        if (LDLib.isEmiLoaded()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            EmiScreenManager.render(EmiDrawContext.wrap(graphics), mouseX, mouseY, partialTicks);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }

        modularUI.rootElement.drawInForeground(graphics, mouseX, mouseY, partialTicks);

        if (Platform.isDevEnv()) {
            renderDebugInfo(graphics, mouseX, mouseY, partialTicks);
        }

        if (draggingElement != null) {
            draggingElement.getB().draw(graphics, mouseX, mouseY, mouseX - 20, mouseY - 20, 40, 40, partialTicks);
        }

        graphics.bufferSource().endBatch();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        PoseStack posestack = graphics.pose();
        posestack.pushPose();
        posestack.translate(leftPos, topPos, 232);

        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(this, graphics, mouseX, mouseY));

        // TODO Native Slot
//        renderItemStackOnMouse(graphics, mouseX, mouseY);
//        renderReturningItemStack(graphics);

        graphics.bufferSource().endBatch();
        posestack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        if (LDLib.isEmiLoaded()) {
            posestack.pushPose();
            posestack.translate(0, 0, 200);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            EmiScreenManager.drawForeground(EmiDrawContext.wrap(graphics), mouseX, mouseY, partialTicks);
            posestack.popPose();
        }

        if (draggingElement == null && tooltipTexts != null && !tooltipTexts.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            DrawerHelper.drawTooltip(graphics, mouseX, mouseY, tooltipTexts, tooltipStack, tooltipComponent, tooltipFont == null ? Minecraft.getInstance().font : tooltipFont);
            graphics.pose().popPose();
        }
    }

    public void renderDebugInfo(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var hovered = modularUI.rootElement.getHoverElement(mouseX, mouseY);
        var x = 2;
        var y = 2;
        var font = Minecraft.getInstance().font;
        if (hovered != null) {
            graphics.drawString(font, "hovered element:", x, y, 0xffff0000, true);
            x += 10;
            y += 10;
            for (var info : hovered.getDebugInfo()) {
                graphics.drawString(font, info, x, y, 0xffff0000, true);
                y += 10;
            }
            x -= 10;
        }
    }

}
