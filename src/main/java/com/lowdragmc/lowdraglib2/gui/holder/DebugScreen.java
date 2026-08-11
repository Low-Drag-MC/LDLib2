package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.client.font.LDFonts;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

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
        ModularUIClientAccess.enableDebugger(targetUI, false);
        uiDebugger.removeSelf();
        targetUI = target;
        uiDebugger = ModularUIClientAccess.acquireDebugger(target);
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
            var font = LDFonts.font();
            DrawerHelperClient.drawSolidRect(guiContext, 0, mouseY - 1, getModularUI().getScreenWidth(), 1, 0xffff0000);
            DrawerHelperClient.drawSolidRect(guiContext, mouseX - 1, 0, 1, getModularUI().getScreenHeight(), 0xffff0000);
            LDFonts.drawText(guiContext, font, "pos(%d, %d)".formatted(mouseX, mouseY),
                    mouseX, Math.max(0, mouseY - 10), ColorPattern.YELLOW.color, true);
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
        // A UI in its own window already ticks itself from its render loop; ticking it here too
        // would run every animation and timer at double speed.
        if (isTargetLocal()) {
            targetUI.tick();
        }
    }
}
