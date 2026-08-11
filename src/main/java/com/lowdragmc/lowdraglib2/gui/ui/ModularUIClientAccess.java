package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.holder.DebugScreen;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
        return onFilesDrop(modularUI, files, UISurface.main());
    }

    /**
     * As {@link #onFilesDrop(ModularUI, List)}, but against a UI that is not hosted in the game
     * window — the cursor has to be queried from that window and scaled by its own size.
     */
    public static boolean onFilesDrop(ModularUI modularUI, List<File> files, UISurface surface) {
        if (files.isEmpty()) return false;
        var x = new double[1];
        var y = new double[1];
        GLFW.glfwGetCursorPos(surface.windowHandle(), x, y);
        var mouseX = x[0] * surface.guiScaledWidth() / surface.screenWidth();
        var mouseY = y[0] * surface.guiScaledHeight() / surface.screenHeight();

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

    /**
     * Finds the {@link ModularUI} behind a screen, however it got there — a screen that is itself a
     * holder, a menu-backed {@code AbstractContainerScreen}, or a widget attached to some other
     * screen.
     *
     * <p>Lives here because "is there an LDLib2 UI on screen right now" is a question any mod can
     * need to ask, and answering it means knowing all three attachment routes.
     */
    @Nullable
    public static ModularUI of(@Nullable Screen screen) {
        if (screen == null) return null;
        if (screen instanceof IModularUIHolder holder && holder.hasModularUI()) {
            return holder.getModularUI();
        }
        if (screen instanceof AbstractContainerScreen<?> container
                && container.getMenu() instanceof IModularUIHolder holder && holder.hasModularUI()) {
            return holder.getModularUI();
        }
        for (var child : screen.children()) {
            if (child instanceof IModularUIHolder holder && holder.hasModularUI()) {
                return holder.getModularUI();
            }
        }
        return null;
    }

    /**
     * Marks this UI as being inspected and returns its debugger, without opening a debug screen.
     *
     * <p>{@link #enableDebugger} pushes a screen, which is right when the user asks for one but wrong
     * when a screen that already exists wants to retarget itself at this UI — a floating window's UI,
     * say. That case needs the debugger, not a second screen.
     */
    public static UIDebugger acquireDebugger(ModularUI modularUI) {
        modularUI.setDebugMode(true);
        var state = getState(modularUI);
        if (state.uiDebuggerCache == null) {
            state.uiDebuggerCache = new UIDebugger(modularUI);
        }
        return state.uiDebuggerCache;
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
