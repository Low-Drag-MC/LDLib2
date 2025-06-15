package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class ModularUIContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public final ModularUI modularUI;

    public ModularUIContainerScreen(ModularUI modularUI, T container, Inventory inventory, Component title) {
        super(container, inventory, title);
        this.modularUI = modularUI;
        modularUI.setScreen(this);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        modularUI.tick();
    }

    @Override
    public void init() {
        this.modularUI.init(width, height);
        this.imageWidth = (int) modularUI.getWidth();
        this.imageHeight = (int) modularUI.getHeight();
        this.addRenderableWidget(modularUI);
        super.init();
    }

    @Override
    public void removed() {
        super.removed();
        modularUI.onRemoved();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

    }

    public boolean shouldCloseOnEsc() {
        return modularUI.shouldCloseOnEsc();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (modularUI.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        modularUI.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!modularUI.shouldCloseOnKeyInventory()) {
            InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
            if (minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
                return modularUI.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        if (Platform.isDevEnv()) {
            modularUI.renderDebugInfo(graphics, mouseX, mouseY, partialTicks);

        }
    }

}
