package com.lowdragmc.lowdraglib2.gui.holder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.annotation.ParametersAreNonnullByDefault;

@Environment(EnvType.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIContainerScreen extends AbstractContainerScreen<ModularUIContainerMenu> {

    public ModularUIContainerScreen(ModularUIContainerMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    public void init() {
        var mui = getMenu().getModularUI();
        if (mui != null) {
            // Initialize ModularUI with screen dimensions BEFORE super.init(), matching NeoForge's ScreenEvent.Init.Pre behavior.
            // This ensures stylesheets and layout are resolved before the screen finalizes its state.
            mui.setScreenAndInit(this);
            this.imageWidth = (int) mui.getWidth();
            this.imageHeight = (int) mui.getHeight();
        }
        super.init();
        if (mui != null) {
            this.addRenderableWidget(mui.getWidget());
            setFocused(mui.getWidget());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

    }

}
