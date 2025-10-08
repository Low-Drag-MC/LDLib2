package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIContainerScreen extends AbstractContainerScreen<ModularUIContainerMenu> {

    public ModularUIContainerScreen(ModularUIContainerMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
        container.modularUI.setScreen(this);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        menu.modularUI.tick();
        menu.modularUI.syncManager.tick();
    }

    @Override
    public void init() {
        var modularUI = menu.modularUI;
        modularUI.init(width, height);
        this.imageWidth = (int) modularUI.getWidth();
        this.imageHeight = (int) modularUI.getHeight();
        this.addRenderableWidget(modularUI.getWidget());
        super.init();
    }

    @Override
    public void removed() {
        super.removed();
        menu.modularUI.onRemoved();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

    }

    public boolean shouldCloseOnEsc() {
        return menu.modularUI.shouldCloseOnEsc();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (menu.modularUI.getWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        menu.modularUI.getWidget().mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!menu.modularUI.shouldCloseOnKeyInventory()) {
            InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
            if (minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
                return menu.modularUI.getWidget().keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isHovering(Slot slot, double mouseX, double mouseY) {
        if (menu.modularUI.isHoverSlot(slot)) {
            return true;
        }
        return super.isHovering(slot, mouseX, mouseY);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (menu.isItemSlot(slot)) {
            return;
        }
        super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (menu.isItemSlot(slot)) {
            return;
        }
        super.renderSlot(guiGraphics, slot);
    }

}
