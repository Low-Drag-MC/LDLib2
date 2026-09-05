package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebuggerWindow;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
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

/**
 * The debugger's default host: a layer pushed over the game, with the {@link UIDebugger} floating in
 * it as a draggable panel. This is what {@link ModularUIClientAccess#enableDebugger} opens — one
 * keypress and nothing else to know.
 *
 * <p>It pays for that with the thing {@link UIDebuggerWindow} exists to fix: it covers the very UI
 * being inspected, cannot be larger than the game window, and has to forward every input event it
 * does not want down to that UI by hand. The window toggle in the title bar moves the debugger
 * across; see {@link ModularUIClientAccess#setDebuggerWindowed}.
 *
 * <p>What this screen does <em>not</em> do is draw the debugger's overlay or run its element picker.
 * Both are the inspected UI's business, drawn and handled inside its own frame, which is what lets
 * the debugger work against a UI in another window at all. Here that shows up as a simplification:
 * the picker below can point at a floating window's UI and the outlines appear in that window, where
 * they belong, rather than being suppressed as they used to be.
 */
@ParametersAreNonnullByDefault
public class DebugScreen extends ModularUIScreen {
    /**
     * Where the pointer really is.
     *
     * <p>Minecraft renders the screens beneath a screen layer with the mouse at {@link Integer#MAX_VALUE}
     * so they cannot report anything as hovered. The inspected UI is one of those, and the debugger
     * needs its hover, so {@code ModularUI} substitutes this back in.
     */
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

        // Floating again: the same element may have just come back from filling a window of its own.
        debugger.setFloating(true);
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
        // Through the flag rather than enableDebugger: nothing is closing, this screen is simply
        // looking elsewhere, and the long way round would pop this very layer on the way past.
        targetUI.setDebugMode(false);
        uiDebugger.removeSelf();
        targetUI = target;
        uiDebugger = ModularUIClientAccess.acquireDebugger(target);
        uiDebugger.setFloating(true);
        modularUI.ui.rootElement.addChildAt(uiDebugger, 0);
        rebuildTargetPicker();
    }

    /**
     * Whether the inspected UI is drawn in this same window.
     *
     * <p>A UI in its own OS window receives its input from that window's event queue, so forwarding
     * this screen's key presses and mouse coordinates into it would be both redundant and aimed at
     * the wrong place. Inspection of the tree still works either way — that is what the picker is
     * for — but the forwarding below is limited to a local target.
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

    /**
     * Ends the debugging session, which is what takes this screen down with it.
     *
     * <p>Deliberately not {@code super.onClose()}: that is {@code setScreen(null)}, which would close
     * the UI being inspected along with the inspector. This screen is only ever a layer over it, and
     * {@link ModularUIClientAccess#enableDebugger} pops exactly that layer.
     */
    @Override
    public void onClose() {
        ModularUIClientAccess.enableDebugger(this.targetUI, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        var keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_F12) {
            onClose();
            return true;
        }
        // Consumed, not merely acted on. The inspected UI answers to the same chords, and everything
        // this screen does not handle is forwarded straight into it - so letting these fall through
        // would toggle each of them twice and leave them exactly as they were.
        if (keyCode == GLFW.GLFW_KEY_F1) {
            uiDebugger.setFocusMode(!uiDebugger.isFocusMode());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F4) {
            uiDebugger.setRenderUIShaping(!uiDebugger.isRenderUIShaping());
            return true;
        }
        if (!super.keyPressed(event)) {
            return isTargetLocal() && ModularUIClientAccess.getWidget(targetUI).keyPressed(event);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!super.charTyped(event)) {
            return isTargetLocal() && ModularUIClientAccess.getWidget(targetUI).charTyped(event);
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (!super.keyReleased(event)) {
            return isTargetLocal() && ModularUIClientAccess.getWidget(targetUI).keyReleased(event);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return isTargetLocal()
                    && ModularUIClientAccess.getWidget(targetUI).mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!super.mouseDragged(event, dx, dy)) {
            return isTargetLocal() && ModularUIClientAccess.getWidget(targetUI).mouseDragged(event, dx, dy);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!ModularUIClientAccess.getWidget(modularUI).mouseReleased(event)) {
            return isTargetLocal() && ModularUIClientAccess.getWidget(targetUI).mouseReleased(event);
        }
        return true;
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!ModularUIClientAccess.getWidget(modularUI).mouseClicked(event, doubleClick)) {
            // Focus mode is not special-cased here: the forward below reaches the inspected UI's own
            // click handling, which is where the pick happens and where it swallows the press.
            return isTargetLocal() && ModularUIClientAccess.getWidget(targetUI).mouseClicked(event, doubleClick);
        } else {
            ModularUIClientAccess.getWidget(modularUI).setFocused(true);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        if (isTargetLocal()) {
            ModularUIClientAccess.getWidget(targetUI).mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        REAL_MOUSE_POS.set(mouseX, mouseY);
        // No depth juggling, unlike 1.21: 26.1's gui renderer is deferred and this layer's own render
        // states are appended after the ones the layer beneath contributed, so they already sort on
        // top of the inspected UI and of the debugger overlay it drew.
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
        // Last, and outside any element walk: moving the debugger into a window reparents it and pops
        // this layer, which is not something to do from inside the toggle's own click dispatch.
        ModularUIClientAccess.applyPendingDebuggerHost(targetUI);
    }
}
