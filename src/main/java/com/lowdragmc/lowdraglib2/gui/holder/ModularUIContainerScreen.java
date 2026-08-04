package com.lowdragmc.lowdraglib2.gui.holder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIContainerScreen extends AbstractContainerScreen<ModularUIContainerMenu> {

    public ModularUIContainerScreen(ModularUIContainerMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    public void init() {
        // the modular widget has already added + init by events
        this.imageWidth = (int) getMenu().getModularUI().getWidth();
        this.imageHeight = (int) getMenu().getModularUI().getHeight();
        super.init();
        // initial focus
        setFocused(getMenu().modularUI.getWidget());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {

    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (!getMenu().getModularUI().onFilesDrop(paths.stream().map(Path::toFile).toList())) {
            super.onFilesDrop(paths);
        }
    }
}
