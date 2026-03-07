package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

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
    public boolean keyPressed(KeyEvent event) {
        var keyCode = event.key();
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
        if (!super.keyPressed(event)) {
            return targetUI.getWidget().keyPressed(event);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!super.charTyped(event)) {
            return targetUI.getWidget().charTyped(event);
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (!super.keyReleased(event)) {
            return targetUI.getWidget().keyReleased(event);
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
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!super.mouseDragged(event, dx, dy)) {
            return targetUI.getWidget().mouseDragged(event, dx, dy);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!modularUI.getWidget().mouseReleased(event)) {
            return targetUI.getWidget().mouseReleased(event);
        }
        return true;
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!modularUI.getWidget().mouseClicked(event, doubleClick)) {
            if (uiDebugger.isFocusMode()) {
                var lastHovered = targetUI.getLastHoveredElement();
                if (lastHovered != null) {
                    uiDebugger.focusElement(lastHovered);
                    return true;
                }
                return false;
            }
            return targetUI.getWidget().mouseClicked(event, doubleClick);
        } else {
            modularUI.getWidget().setFocused(true);
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

        var guiContext = GUIContext.of(graphics, mouseX, mouseY, partialTick);

        UIElement shapingUI = null;
        var isChildrenHovered = modularUI.getLastHoveredElement() != null && !modularUI.ui.rootElement.isHover();
        if (uiDebugger.isFocusMode() && !isChildrenHovered) {
            shapingUI = targetUI.getLastHoveredElement();
        }
        if (shapingUI == null && uiDebugger.isRenderUIShaping() && uiDebugger.hierarchy.treeList.getHoveredNode() != null) {
            shapingUI = uiDebugger.hierarchy.treeList.getHoveredNode().key;
        }
        if (shapingUI != null) {
            targetUI.getWidget().renderUISpacing(guiContext, shapingUI, graphics);
        }

        if (!isChildrenHovered) {
            // draw cursor
            var font = Minecraft.getInstance().font;
            DrawerHelper.drawSolidRect(guiContext, 0, mouseY - 1, getModularUI().getScreenWidth(), 1, 0xffff0000);
            DrawerHelper.drawSolidRect(guiContext, mouseX - 1, 0, 1, getModularUI().getScreenHeight(), 0xffff0000);
            graphics.drawString(font, "pos(%d, %d)".formatted(mouseX, mouseY), mouseX, Math.max(0, mouseY - 10), ColorPattern.YELLOW.color, true);
        }


        if (shapingUI != null) {
            var x = 0;
            var y = 0;
            for (var info : shapingUI.getDebugInfo()) {
                graphics.drawString(font, info, x, y, -1, true);
                y += 10;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        targetUI.tick();
    }
}
