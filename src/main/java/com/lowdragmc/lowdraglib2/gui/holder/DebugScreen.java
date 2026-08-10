package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DebugScreen extends ModularUIScreen {
    public final static Vector2i REAL_MOUSE_POS = new Vector2i();
    public ModularUI targetUI;
    public UIDebugger uiDebugger;

    /**
     * Lets the user pick which UI to inspect when more than one is on screen at once — the game
     * window's, or any UI living in its own OS window. Absolutely positioned over the debugger rather
     * than laid out beside it, so the debugger's own layout is untouched.
     */
    private final UIElement targetPicker = new UIElement();

    /**
     * The UI drawn in the game window, remembered at construction so the picker can always offer a
     * way back to it. Null when the debugger was opened straight from a floating window.
     */
    @Nullable
    private final ModularUI localUI;

    public DebugScreen(UIDebugger debugger) {
        super(ModularUI.of(UI.of(new UIElement().layout(layout -> layout.widthPercent(100).heightPercent(100)),
                        StylesheetManager.INSTANCE.getStylesheet(StylesheetManager.MODERN))),
                Component.literal("Debug Screen"));
        this.uiDebugger = debugger;
        this.targetUI = debugger.modularUI;
        this.localUI = ModularUIWindow.windowOf(targetUI) == null ? targetUI : null;

        this.targetPicker.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.top(0);
            layout.left(0);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(2);
            layout.paddingAll(2);
        }).getStyle().zIndex(500);

        this.modularUI.ui.rootElement.addChild(uiDebugger);
        this.modularUI.ui.rootElement.addChild(targetPicker);
        rebuildTargetPicker();
    }

    /**
     * Points the debugger at another UI. The previous one stops reporting itself as debugged, and the
     * new one hands over its debugger without opening a second screen.
     */
    public void setTarget(ModularUI target) {
        if (target == targetUI) return;
        targetUI.enableDebugger(false);
        uiDebugger.removeSelf();
        targetUI = target;
        uiDebugger = target.acquireDebugger();
        modularUI.ui.rootElement.addChildAt(uiDebugger, 0);
        rebuildTargetPicker();
    }

    /**
     * Whether the inspected UI is drawn in this same window.
     *
     * <p>A UI in its own OS window is laid out against that window's size and presented somewhere
     * else entirely, so forwarding this screen's mouse coordinates into it, or drawing its element
     * outlines here, would point at the wrong place. Inspection of the tree still works — that is
     * what the picker is for — but the pointer-driven parts are limited to a local target.
     */
    public boolean isTargetLocal() {
        return targetUI == localUI;
    }

    private void rebuildTargetPicker() {
        targetPicker.clearAllChildren();
        var candidates = new LinkedHashMap<String, ModularUI>();
        if (localUI != null) {
            candidates.put("Game Window", localUI);
        }
        for (var window : ModularUIWindow.openWindows()) {
            candidates.put(window.getTitle(), window.getModularUI());
        }
        // One candidate means there is nothing to choose between; do not spend screen space on it.
        targetPicker.setDisplay(candidates.size() > 1);
        if (candidates.size() <= 1) return;
        candidates.forEach((label, ui) -> {
            if (ui == null) return;
            var selected = ui == targetUI;
            targetPicker.addChild(new Button()
                    .setText(Component.literal(selected ? "[" + label + "]" : label))
                    .setOnClick(e -> setTarget(ui))
                    .layout(layout -> layout.height(14)));
        });
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
            return isTargetLocal() && targetUI.getWidget().keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!super.charTyped(codePoint, modifiers)) {
            return isTargetLocal() && targetUI.getWidget().charTyped(codePoint, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!super.keyReleased(keyCode, scanCode, modifiers)) {
            return isTargetLocal() && targetUI.getWidget().keyReleased(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return isTargetLocal() && targetUI.getWidget().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return isTargetLocal() && targetUI.getWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!super.mouseReleased(mouseX, mouseY, button)) {
            return isTargetLocal() && targetUI.getWidget().mouseReleased(mouseX, mouseY, button);
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
            return isTargetLocal() && targetUI.getWidget().mouseClicked(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        if (isTargetLocal()) {
            targetUI.getWidget().mouseMoved(mouseX, mouseY);
        }
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
        // Outlines are drawn in this window's coordinate space, which only matches the target when
        // the target is drawn here too.
        if (shapingUI != null && isTargetLocal()) {
            targetUI.getWidget().renderUISpacing(shapingUI, graphics);
        }

        if (!isChildrenHovered) {
            // draw cursor
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);
            var font = LDLibFonts.font();
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
        // A UI in its own window already ticks itself from its render loop; ticking it here too
        // would run every animation and timer at double speed.
        if (isTargetLocal()) {
            targetUI.tick();
        }
    }
}
