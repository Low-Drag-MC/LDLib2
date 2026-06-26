package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
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
        ModularUIClientAccess.enableDebugger(this.targetUI, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        var keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_F12) {
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
            return ModularUIClientAccess.getWidget(targetUI).keyPressed(event);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!super.charTyped(event)) {
            return ModularUIClientAccess.getWidget(targetUI).charTyped(event);
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (!super.keyReleased(event)) {
            return ModularUIClientAccess.getWidget(targetUI).keyReleased(event);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return ModularUIClientAccess.getWidget(targetUI).mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!super.mouseDragged(event, dx, dy)) {
            return ModularUIClientAccess.getWidget(targetUI).mouseDragged(event, dx, dy);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!ModularUIClientAccess.getWidget(modularUI).mouseReleased(event)) {
            return ModularUIClientAccess.getWidget(targetUI).mouseReleased(event);
        }
        return true;
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!ModularUIClientAccess.getWidget(modularUI).mouseClicked(event, doubleClick)) {
            if (uiDebugger.isFocusMode()) {
                var lastHovered = targetUI.getLastHoveredElement();
                if (lastHovered != null) {
                    uiDebugger.focusElement(lastHovered);
                    return true;
                }
                return false;
            }
            return ModularUIClientAccess.getWidget(targetUI).mouseClicked(event, doubleClick);
        } else {
            ModularUIClientAccess.getWidget(modularUI).setFocused(true);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        ModularUIClientAccess.getWidget(targetUI).mouseMoved(mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
            ModularUIClientAccess.getWidget(targetUI).renderUISpacing(guiContext, shapingUI, graphics);
        }

        if (!isChildrenHovered) {
            // draw cursor
            var font = Minecraft.getInstance().font;
            DrawerHelperClient.drawSolidRect(guiContext, 0, mouseY - 1, getModularUI().getScreenWidth(), 1, 0xffff0000);
            DrawerHelperClient.drawSolidRect(guiContext, mouseX - 1, 0, 1, getModularUI().getScreenHeight(), 0xffff0000);
            graphics.text(font, "pos(%d, %d)".formatted(mouseX, mouseY), mouseX, Math.max(0, mouseY - 10), ColorPattern.YELLOW.color, true);
        }


        if (shapingUI != null) {
            var x = 0;
            var y = 0;
            for (var info : shapingUI.getDebugInfo()) {
                graphics.text(font, info, x, y, -1, true);
                y += 10;
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        targetUI.tick();
    }
}
