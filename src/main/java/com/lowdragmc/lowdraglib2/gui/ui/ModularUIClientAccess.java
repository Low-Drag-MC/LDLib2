package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.holder.DebugScreen;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebuggerWindow;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;

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

    /**
     * Runs one of the {@link com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents} against this UI,
     * exactly as its key chord would.
     *
     * <p>For a keymap that resolved the chord itself: the action still has to reach whatever holds the
     * selection, and that routing lives in one place.
     *
     * @return true if anything handled it.
     */
    public static boolean dispatchCommand(ModularUI modularUI, String command) {
        return getWidget(modularUI).dispatchCommand(command,
                modularUI.lastPressedKeyCode, modularUI.lastPressedShortcutKey, modularUI.lastPressedModifiers);
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
     * Routes files dropped onto the game window from outside the game to the element under the cursor,
     * as a {@link UIEvents#FILE_DROP} event that bubbles up from it.
     * <p>
     * The cursor position is queried from the platform rather than taken from the last mouse move: the
     * operating system does not deliver mouse movement while a drag from another application is in
     * progress, so the cached hover element is whatever was under the cursor before the drag began.
     *
     * @return true if any element handled the drop.
     */
    public static boolean onFilesDrop(ModularUI modularUI, List<File> files) {
        var surface = UISurface.main();
        var cursor = queryCursor(surface.windowHandle());
        return onFilesDrop(modularUI, files, surface, cursor[0], cursor[1]);
    }

    /**
     * As {@link #onFilesDrop(ModularUI, List)}, for a drop whose position is already known — SDL reports
     * where a drop landed in a window LDLib2 owns — in that window's coordinates, which are scaled by the
     * surface's own size.
     */
    public static boolean onFilesDrop(ModularUI modularUI, List<File> files, UISurface surface, double windowX, double windowY) {
        if (files.isEmpty()) return false;
        var mouseX = windowX * surface.guiScaledWidth() / Math.max(1, surface.screenWidth());
        var mouseY = windowY * surface.guiScaledHeight() / Math.max(1, surface.screenHeight());

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
     * The pointer's position relative to {@code windowHandle}, in that window's coordinates. Read from
     * the global position rather than SDL's per-window mouse state, which only follows the window that
     * has mouse focus and is not updated during a drag from another application.
     */
    public static double[] queryCursor(long windowHandle) {
        try (var stack = MemoryStack.stackPush()) {
            var globalX = stack.mallocFloat(1);
            var globalY = stack.mallocFloat(1);
            SDLMouse.SDL_GetGlobalMouseState(globalX, globalY);
            var windowX = stack.callocInt(1);
            var windowY = stack.callocInt(1);
            SDLVideo.SDL_GetWindowPosition(windowHandle, windowX, windowY);
            return new double[]{globalX.get(0) - windowX.get(0), globalY.get(0) - windowY.get(0)};
        }
    }

    // ------------------------------------------------------------------------------------ text input

    /**
     * Switches text input on or off for the window hosting {@code modularUI}, to match whether its
     * focused element is one the user types into.
     *
     * <p>Under SDL a window receives no typed characters at all — and no input method runs — until text
     * input is started for it, so this is what makes a {@code TextField} work, not an optimisation.
     * The game window goes through vanilla's {@code TextInputManager} with this UI's widget as the owner,
     * the same way an {@code EditBox} does, so a vanilla field focused afterwards takes over cleanly and
     * losing focus here never switches off input a vanilla field still needs. A UI hosted in its own
     * operating-system window switches that window instead.
     *
     * <p>Called when the focus moves and again every frame, so an element that becomes read-only while
     * focused lets go too. Cheap when nothing changed.
     */
    public static void syncTextInput(ModularUI modularUI) {
        var state = getState(modularUI);
        var focused = modularUI.focusedElement;
        var wanted = !modularUI.isRemoved() && focused != null && focused.isTextInput();
        if (wanted == state.textInputActive) return;
        state.textInputActive = wanted;
        var window = ModularUIWindow.windowOf(modularUI);
        if (window != null) {
            if (!window.isOpen()) return;
            if (wanted) {
                window.window().startTextInput();
            } else {
                window.window().stopTextInput();
            }
        } else {
            Minecraft.getInstance().onTextInputFocusChange(state.getWidget(), wanted);
        }
    }

    /**
     * Lets go of text input for good, when the UI is removed.
     */
    static void releaseTextInput(ModularUI modularUI) {
        var state = getState(modularUI);
        if (!state.textInputActive) return;
        state.textInputActive = false;
        var window = ModularUIWindow.windowOf(modularUI);
        if (window != null) {
            if (window.isOpen()) {
                window.window().stopTextInput();
            }
        } else {
            Minecraft.getInstance().textInputManager().stopTextInput(state.getWidget());
        }
    }

    /**
     * Hands an input-method composition to the focused element as a {@link UIEvents#PREEDIT} event.
     *
     * @param preedit the composition, or {@code null} when it has ended
     * @return true if the focused element handled it
     */
    public static boolean dispatchPreedit(ModularUI modularUI, @Nullable PreeditEvent preedit) {
        var focused = modularUI.focusedElement;
        if (focused == null || !focused.isTextInput()) return false;
        var event = UIEvent.create(UIEvents.PREEDIT);
        event.customData = preedit;
        event.hasCapturePhase = false;
        event.hasBubblePhase = false;
        event.target = focused;
        UIEventDispatcher.dispatchEvent(event);
        return event.hasHandler;
    }

    /**
     * Tells the input method where the text being edited is, so its candidate list opens beside it
     * rather than in a corner of the screen. Coordinates are gui units on the surface hosting the UI.
     */
    public static void setTextInputArea(ModularUI modularUI, float x0, float y0, float x1, float y1) {
        var window = ModularUIWindow.windowOf(modularUI);
        if (window != null) {
            window.setTextInputArea(x0, y0, x1, y1);
        } else {
            Minecraft.getInstance().textInputManager().setTextInputArea(
                    (int) Math.floor(x0), (int) Math.floor(y0), (int) Math.ceil(x1), (int) Math.ceil(y1));
        }
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
     * This UI's debugger, or {@code null} if it has never had one. Creates nothing and enables
     * nothing, unlike {@link #acquireDebugger} — so asking is never what starts a session.
     */
    @Nullable
    public static UIDebugger getUiDebugger(ModularUI modularUI) {
        return getState(modularUI).uiDebuggerCache;
    }

    /**
     * Marks this UI as being inspected and returns its debugger, without opening a host for it.
     *
     * <p>{@link #enableDebugger} pushes a screen, which is right when the user asks for one but wrong
     * when a host that already exists wants to retarget itself at this UI — a floating window's UI,
     * say. That case needs the debugger, not a second host.
     */
    public static UIDebugger acquireDebugger(ModularUI modularUI) {
        modularUI.setDebugMode(true);
        var state = getState(modularUI);
        if (state.uiDebuggerCache == null) {
            state.uiDebuggerCache = new UIDebugger(modularUI);
        }
        return state.uiDebuggerCache;
    }

    /**
     * Opens or closes a debugger on this UI.
     *
     * <p>It opens where it always has, as a {@link DebugScreen} layered over the game — that is one
     * keypress with nothing else to know. The title bar's window toggle then moves it into an OS
     * window of its own, which is where it stops covering the UI it inspects; see
     * {@link #setDebuggerWindowed}.
     */
    public static void enableDebugger(ModularUI modularUI, boolean debugMode) {
        if (modularUI.isDebugMode() == debugMode) {
            return;
        }
        modularUI.setDebugMode(debugMode);
        if (debugMode) {
            Minecraft.getInstance().gui.pushScreenLayer(new DebugScreen(acquireDebugger(modularUI)));
        } else {
            // Both hosts, because either could be the one showing it. Leaving the other open would
            // put a debugger on screen that its own target no longer believes in.
            UIDebuggerWindow.closeFor(modularUI);
            dismissDebugScreen(modularUI);
        }
    }

    /**
     * Pops the debug screen layer, if the top one is this UI's.
     *
     * <p>Guarded twice over. Only a layer of ours, because something else may have been pushed above
     * it and popping that would be a different screen disappearing; and only the debugger this UI
     * currently owns, because a debug screen that has been retargeted elsewhere is no longer ours to
     * close.
     */
    private static void dismissDebugScreen(ModularUI modularUI) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof DebugScreen debugScreen
                && debugScreen.uiDebugger == getState(modularUI).uiDebuggerCache) {
            minecraft.gui.popScreenLayer();
        }
    }

    /** Whether this UI's debugger is in an OS window of its own rather than layered over the game. */
    public static boolean isDebuggerWindowed(ModularUI modularUI) {
        return UIDebuggerWindow.windowFor(modularUI) != null;
    }

    /**
     * Moves an open debugger between its two hosts, without ending the session.
     *
     * <p>The order matters in both directions and is the whole content of this method: the incoming
     * host adopts the debugger element <em>before</em> the outgoing one goes away, or the outgoing
     * one takes it down with it — popping a screen layer runs {@code removed()} over everything still
     * parented under it.
     *
     * <p>Requesting a window can fail (native fullscreen, an SDL video driver with no windows), in
     * which case nothing moves and the debugger stays where it is.
     */
    public static void setDebuggerWindowed(ModularUI modularUI, boolean windowed) {
        var state = getState(modularUI);
        if (!modularUI.isDebugMode() || state.uiDebuggerCache == null
                || windowed == isDebuggerWindowed(modularUI)) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (windowed) {
            if (!UIDebuggerWindow.openFor(state.uiDebuggerCache)) return;
            dismissDebugScreen(modularUI);
        } else {
            minecraft.gui.pushScreenLayer(new DebugScreen(state.uiDebuggerCache));
            var window = UIDebuggerWindow.windowFor(modularUI);
            if (window != null) {
                window.handOff();
            }
        }
    }

    /**
     * Applies a host switch the debugger's title bar asked for.
     *
     * <p>Called by whichever host is showing it, between frames. The request is queued rather than
     * acted on because it arrives from inside the toggle's own click dispatch, and switching hosts
     * reparents the whole debugger — the toggle's own great-grandparent — out from under it.
     */
    public static void applyPendingDebuggerHost(ModularUI modularUI) {
        var state = getState(modularUI);
        var pending = state.pendingDebuggerWindowed;
        if (pending == null) return;
        state.pendingDebuggerWindowed = null;
        setDebuggerWindowed(modularUI, pending);
    }

    /**
     * Asks for a host switch on the next frame.
     *
     * @see #applyPendingDebuggerHost
     */
    public static void requestDebuggerWindowed(ModularUI modularUI, boolean windowed) {
        getState(modularUI).pendingDebuggerWindowed = windowed;
    }

    /**
     * This UI's debugger while a session is actually open on it, or {@code null}.
     *
     * <p>Not {@link #getUiDebugger}: that one answers for a debugger this UI has merely had at some
     * point, which every caller here would then have to pair with a {@code isDebugMode()} of its own.
     */
    @Nullable
    static UIDebugger activeDebugger(ModularUI modularUI) {
        return modularUI.isDebugMode() ? getState(modularUI).uiDebuggerCache : null;
    }

    /**
     * The debugger that has turned the pointer into an element inspector, or {@code null} when a
     * press on this UI should activate rather than select.
     */
    @Nullable
    static UIDebugger pickingDebugger(ModularUI modularUI) {
        var debugger = activeDebugger(modularUI);
        return debugger != null && debugger.isFocusMode() ? debugger : null;
    }
}
