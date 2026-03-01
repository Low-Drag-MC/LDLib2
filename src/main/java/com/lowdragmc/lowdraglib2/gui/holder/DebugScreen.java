package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DebugScreen extends ModularUIScreen {
    public final static Vector2i REAL_MOUSE_POS = new Vector2i();
    public final ModularUI targetUI;
    public final UIDebugger uiDebugger;

    public DebugScreen(UIDebugger debugger) {
        super(ModularUI.of(UI.of(new UIElement().layout(layout -> layout.widthPercent(100).heightPercent(100)),
                        StylesheetManager.INSTANCE.getStylesheet(StylesheetManager.MODERN))),
                Component.literal("Debug Screen"));
        this.uiDebugger = debugger;
        this.targetUI = debugger.modularUI;

        this.modularUI.ui.rootElement.addChild(uiDebugger);
    }

    @Override
    public void onClose() {
        super.onClose();
        this.targetUI.enableDebugger(false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F3) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F1) {
            uiDebugger.setFocusMode(!uiDebugger.isFocusMode());
        }
        if (keyCode == GLFW.GLFW_KEY_F4) {
            uiDebugger.setRenderUIShaping(!uiDebugger.isRenderUIShaping());
        }
        if (!super.keyPressed(keyCode, scanCode, modifiers)) {
            return targetUI.getWidget().keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!super.charTyped(codePoint, modifiers)) {
            return targetUI.getWidget().charTyped(codePoint, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!super.keyReleased(keyCode, scanCode, modifiers)) {
            return targetUI.getWidget().keyReleased(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return targetUI.getWidget().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return targetUI.getWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!super.mouseReleased(mouseX, mouseY, button)) {
            return targetUI.getWidget().mouseReleased(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) {
            if (uiDebugger.isFocusMode()) {
                var lastHovered = targetUI.getLastHoveredElement();
                if (lastHovered != null) {
                    uiDebugger.focusElement(lastHovered);
                    return true;
                }
                return false;
            }
            return targetUI.getWidget().mouseClicked(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        targetUI.getWidget().mouseMoved(mouseX, mouseY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        REAL_MOUSE_POS.set(mouseX, mouseY);

        UIElement shapingUI = null;
        var isChildrenHovered = modularUI.getLastHoveredElement() != null && !modularUI.ui.rootElement.isHover();
        if (uiDebugger.isFocusMode() && !isChildrenHovered) {
            shapingUI = targetUI.getLastHoveredElement();
        }
        if (shapingUI == null && uiDebugger.isRenderUIShaping() && uiDebugger.hierarchy.treeList.getHoveredNode() != null) {
            shapingUI = uiDebugger.hierarchy.treeList.getHoveredNode().key;
        }
        if (shapingUI != null) {
            targetUI.getWidget().renderUISpacing(shapingUI, graphics);
        }

        if (!isChildrenHovered) {
            // draw cursor
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);
            var font = Minecraft.getInstance().font;
            DrawerHelper.drawSolidRect(graphics, 0, mouseY - 1, getModularUI().getScreenWidth(), 1, 0xffff0000);
            DrawerHelper.drawSolidRect(graphics, mouseX - 1, 0, 1, getModularUI().getScreenHeight(), 0xffff0000);
            graphics.drawString(font, "pos(%d, %d)".formatted(mouseX, mouseY), mouseX, Math.max(0, mouseY - 10), ColorPattern.YELLOW.color, true);
            graphics.pose().popPose();
        }


        if (shapingUI != null) {
            var x = 0;
            var y = 0;
            for (var info : shapingUI.getDebugInfo()) {
                graphics.drawString(font, info, x, y, -1, true);
                y += 10;
            }
        }

        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        targetUI.tick();
    }
}
