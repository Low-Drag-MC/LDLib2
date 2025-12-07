package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ModularUIJEIWidget implements IRecipeWidget, IJeiGuiEventListener {
    public static final ScreenPosition ZERO = new ScreenPosition(0, 0);
    public final ModularUI modularUI;

    public ModularUIJEIWidget(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    /// IRecipeWidget
    @Override
    public ScreenPosition getPosition() {
        return ModularUIJEIWidget.ZERO;
    }

    @Override
    public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.flush();
        var partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        // get real mouse
        var realMouse = guiGraphics.pose().last().pose().invert(new Matrix4f()).transformPosition(new Vector3f(0, 0, 0));
        modularUI.getWidget().render(guiGraphics, (int) (mouseX - realMouse.x), (int) (mouseY - realMouse.y), partialTick);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltipBuilder, double mouseX, double mouseY) {
        if (!modularUI.getDragHandler().isDragging() && modularUI.getTooltipTexts() != null && !modularUI.getTooltipTexts().isEmpty()) {
            tooltipBuilder.addAll(modularUI.getTooltipTexts());
            if (modularUI.getTooltipComponent() != null) tooltipBuilder.add(modularUI.getTooltipComponent());
        }
    }

    @Override
    public void tick() {
        modularUI.tick();
    }

    /// IJeiGuiEventListener
    @Override
    public ScreenRectangle getArea() {
        return modularUI.getWidget().getRectangle();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        modularUI.getWidget().mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return modularUI.getWidget().mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return modularUI.getWidget().mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return modularUI.getWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return modularUI.getWidget().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(double mouseX, double mouseY, int keyCode, int scanCode, int modifiers) {
        return modularUI.getWidget().keyPressed(keyCode, scanCode, modifiers);
    }
}
