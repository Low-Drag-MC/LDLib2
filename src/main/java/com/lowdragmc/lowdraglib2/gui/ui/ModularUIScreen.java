package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIScreen extends Screen {
    public final ModularUI modularUI;
    /**
     * Starting X position for the Gui. Inconsistent use for Gui backgrounds.
     */
    @Getter
    protected int leftPos;
    /**
     * Starting Y position for the Gui. Inconsistent use for Gui backgrounds.
     */
    @Getter
    protected int topPos;

    public ModularUIScreen(ModularUI modularUI, Component title) {
        super(title);
        this.modularUI = modularUI;
        modularUI.setScreen(this);
    }

    @Override
    public void tick() {
        modularUI.tick();
    }

    @Override
    public void init() {
        this.modularUI.init(width, height);
        this.addRenderableWidget(modularUI.getWidget());
        this.leftPos = (int) ((this.width - modularUI.getWidth()) / 2);
        this.topPos = (int) ((this.height - modularUI.getHeight()) / 2);
        super.init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        modularUI.onRemoved();
    }

    public boolean shouldCloseOnEsc() {
        return modularUI.shouldCloseOnEsc();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (modularUI.getWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        modularUI.getWidget().mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!modularUI.shouldCloseOnKeyInventory()) {
            InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
            if (minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
                return modularUI.getWidget().keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        if (Platform.isDevEnv()) {
            modularUI.getWidget().renderDebugInfo(graphics, mouseX, mouseY, partialTicks);
        }
    }

}
