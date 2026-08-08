package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;
import java.util.List;

@ParametersAreNonnullByDefault
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
        setFocused(ModularUIClientAccess.getWidget(getMenu().modularUI));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // todo
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (!ModularUIClientAccess.onFilesDrop(getMenu().getModularUI(), paths.stream().map(Path::toFile).toList())) {
            super.onFilesDrop(paths);
        }
    }
}
