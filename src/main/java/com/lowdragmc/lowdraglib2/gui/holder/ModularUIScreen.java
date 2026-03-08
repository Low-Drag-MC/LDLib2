package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import lombok.Getter;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIScreen extends Screen {
    @Getter
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
    }

    @Override
    public void init() {
        ModularUIClientAccess.setScreenAndInit(this.modularUI, this);
        this.addRenderableWidget(ModularUIClientAccess.getWidget(modularUI));
        this.leftPos = (int) ((this.width - modularUI.getWidth()) / 2);
        this.topPos = (int) ((this.height - modularUI.getHeight()) / 2);
        super.init();
        // initial focus
        setFocused(ModularUIClientAccess.getWidget(modularUI));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // TODO
    }
}
