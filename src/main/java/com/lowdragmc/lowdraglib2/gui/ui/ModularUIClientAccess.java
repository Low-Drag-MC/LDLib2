package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.holder.DebugScreen;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.List;

public final class ModularUIClientAccess {
    private ModularUIClientAccess() {
    }

    static ModularUIClientState getState(ModularUI modularUI) {
        if (!(modularUI.clientState instanceof ModularUIClientState)) {
            var state = new ModularUIClientState(modularUI);
            modularUI.clientState = state;
            return state;
        }
        return (ModularUIClientState) modularUI.clientState;
    }

    public static void setScreenAndInit(ModularUI modularUI, Screen screen) {
        var state = getState(modularUI);
        state.screen = screen;
        modularUI.init(screen.width, screen.height);
    }

    public static ModularUIWidget getWidget(ModularUI modularUI) {
        return getState(modularUI).getWidget();
    }

    @Nullable
    public static Screen getScreen(ModularUI modularUI) {
        return getState(modularUI).screen;
    }

    @Nullable
    public static HoverTooltips getHoverTooltips(ModularUI modularUI) {
        return getState(modularUI).hoverTooltips;
    }

    public static void setHoverTooltip(ModularUI modularUI, @Nullable HoverTooltips hoverTooltips) {
        getState(modularUI).hoverTooltips = hoverTooltips;
    }

    public static void cleanTooltip(ModularUI modularUI) {
        getState(modularUI).hoverTooltips = null;
    }

    public static List<Rect2i> getGuiExtraAreas(ModularUI modularUI) {
        var state = getState(modularUI);
        if (state.extraAreas.isEmpty()) {
            state.extraAreas.clear();
            UIElementClientAccess.appendExtraAreas(modularUI.ui.rootElement, state.extraAreas);
        }
        return state.extraAreas;
    }

    /**
     * Routes files dropped onto the window from outside the game to the element under the cursor, as a
     * {@link UIEvents#FILE_DROP} event that bubbles up from it.
     * <p>
     * The cursor position is queried from the window rather than taken from the last mouse move: the
     * operating system does not deliver mouse movement while a drag from another application is in
     * progress, so the cached hover element is whatever was under the cursor before the drag began.
     *
     * @return true if any element handled the drop.
     */
    public static boolean onFilesDrop(ModularUI modularUI, List<File> files) {
        if (files.isEmpty()) return false;
        var window = Minecraft.getInstance().getWindow();
        var x = new double[1];
        var y = new double[1];
        GLFW.glfwGetCursorPos(window.handle(), x, y);
        var mouseX = x[0] * window.getGuiScaledWidth() / window.getScreenWidth();
        var mouseY = y[0] * window.getGuiScaledHeight() / window.getScreenHeight();

        var hit = modularUI.ui.rootElement.hitTest(mouseX, mouseY);
        if (hit == null) return false;
        var event = UIEvent.create(UIEvents.FILE_DROP);
        event.x = (float) mouseX;
        event.y = (float) mouseY;
        event.droppedFiles = List.copyOf(files);
        event.target = hit.getA();
        UIEventDispatcher.dispatchEvent(event);
        return event.hasHandler;
    }

    public static void enableDebugger(ModularUI modularUI, boolean debugMode) {
        if (modularUI.isDebugMode() == debugMode) {
            return;
        }
        modularUI.setDebugMode(debugMode);
        if (!debugMode) {
            return;
        }
        var state = getState(modularUI);
        if (state.uiDebuggerCache == null) {
            state.uiDebuggerCache = new UIDebugger(modularUI);
        }
        Minecraft.getInstance().pushGuiLayer(new DebugScreen(state.uiDebuggerCache));
    }
}
