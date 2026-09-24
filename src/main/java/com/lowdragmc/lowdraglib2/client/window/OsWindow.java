package com.lowdragmc.lowdraglib2.client.window;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.RenderSystemAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.PreeditEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A second operating-system window, alongside the game's own.
 *
 * <p>Owns the SDL handle and the queue of events {@link OsWindowEvents} routes to it, and nothing
 * else — no drawing, no UI. Presenting is {@link OsWindowPresenter}; what goes in it is an
 * {@link OsWindowHost}.
 *
 * <p>Everything here runs on the render thread, which is also the process main thread (Minecraft
 * names it "Render thread" but it is the one the JVM started on, and on macOS that is the only
 * thread SDL will accept window calls from). That is what makes creating and destroying windows from
 * a frame hook legal on all three platforms.
 */
public final class OsWindow {

    /**
     * System cursors, shared across every window — an SDL cursor is not owned by one — and created
     * lazily because most windows only ever need the arrow.
     */
    private static final Int2LongMap CURSORS = new Int2LongOpenHashMap();

    /** See {@link #setActivateOnShow(boolean)}. */
    private static boolean activateOnShow = true;

    private final long handle;
    private final int id;
    private final ObjectArrayList<OsWindowEvent> pending = new ObjectArrayList<>();
    /** Paths of a drop in progress: SDL reports one file per event between a begin and a complete. */
    private final List<File> droppingFiles = new ArrayList<>();
    private int currentCursorShape = -1;
    /** {@code InputConstants.MOUSE_BUTTON_*} bits currently held, tracked from this window's own events. */
    private int heldButtons;
    private boolean textInputStarted;

    @Getter
    private int framebufferWidth;
    @Getter
    private int framebufferHeight;
    @Getter
    private int windowWidth;
    @Getter
    private int windowHeight;
    @Getter
    private int positionX;
    @Getter
    private int positionY;
    @Getter
    private boolean focused;
    @Getter
    private double cursorX;
    @Getter
    private double cursorY;
    @Getter
    private boolean destroyed;

    private OsWindow(long handle) {
        this.handle = handle;
        this.id = SDLVideo.SDL_GetWindowID(handle);
    }

    /**
     * Whether a window takes keyboard focus when it is shown.
     *
     * <p>Turned off for the duration of an automated run: a scenario can tear a view out into a real
     * window halfway through, and a window that activates on show takes the keyboard away from
     * whatever the person at the machine is doing. Only <em>activation</em> is governed — the window is
     * still raised, because "show this window behind the others" has no portable spelling.
     */
    public static void setActivateOnShow(boolean activateOnShow) {
        OsWindow.activateOnShow = activateOnShow;
    }

    /**
     * Creates a window the game can present into.
     *
     * <p>Created through the {@code GpuBackend} the game is running on, the same call that made the
     * game's own window, so it carries whatever that backend needs — an OpenGL-capable pixel format that
     * can share the game's one context, or a Vulkan-capable surface — without this class knowing which.
     * Created hidden, so the first frame is presented before anything is shown and the window does not
     * flash white.
     *
     * @param decorated whether the OS draws a title bar and resize border. Undecorated avoids the
     *                  platform's modal move/resize loop, which runs nested inside the event pump and
     *                  freezes the whole game for as long as the user drags the window.
     * @return the window, or {@code null} if the platform refused — an exhausted driver, or a video
     *         driver with no windows at all. Callers are expected to fall back to an in-game panel
     *         rather than treat this as fatal.
     */
    @Nullable
    public static OsWindow create(String title, int width, int height, boolean decorated) {
        RenderSystem.assertOnRenderThread();
        var backend = RenderSystemAccessor.ldlib2$getBackend();
        if (backend == null) {
            LDLib2.LOGGER.warn("[os-window] no graphics backend is running, cannot open '{}'", title);
            return null;
        }
        // Same density flag as the game's own window, so a HiDPI display gets a full-resolution
        // framebuffer here too and a UI drawn at the shared gui scale is not blurred.
        // Before creating it: SDL queues the new window's size and position events during creation,
        // and any that reached vanilla would be applied to the game's own window.
        OsWindowEvents.install();
        var flags = SDLVideo.SDL_WINDOW_HIDDEN | SDLVideo.SDL_WINDOW_RESIZABLE | SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY;
        if (!decorated) {
            flags |= SDLVideo.SDL_WINDOW_BORDERLESS;
        }
        var handle = backend.createWindow(title, Math.max(1, width), Math.max(1, height), flags);
        if (handle == MemoryUtil.NULL) {
            var error = SDLError.SDL_GetError();
            LDLib2.LOGGER.warn("[os-window] could not create '{}' ({}); falling back to an in-game panel",
                    title, error == null || error.isEmpty() ? "no SDL error reported" : error);
            return null;
        }

        var window = new OsWindow(handle);
        window.refreshGeometry();
        OsWindowEvents.register(window);
        return window;
    }

    public long handle() {
        return handle;
    }

    /** SDL's id for the window, which is what its events are addressed by. */
    public int id() {
        return id;
    }

    private void refreshGeometry() {
        try (var stack = MemoryStack.stackPush()) {
            var first = stack.mallocInt(1);
            var second = stack.mallocInt(1);
            if (SDLVideo.SDL_GetWindowSizeInPixels(handle, first, second)) {
                framebufferWidth = first.get(0);
                framebufferHeight = second.get(0);
            }
            if (SDLVideo.SDL_GetWindowSize(handle, first, second)) {
                windowWidth = first.get(0);
                windowHeight = second.get(0);
            }
            if (SDLVideo.SDL_GetWindowPosition(handle, first, second)) {
                positionX = first.get(0);
                positionY = second.get(0);
            }
        }
        focused = (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_INPUT_FOCUS) != 0;
    }

    // ------------------------------------------------------------------------------------ events

    /**
     * Records an SDL event addressed to this window. Called by {@link OsWindowEvents} from inside the
     * event pump, with {@code event} valid only for the duration of the call — so everything needed is
     * copied out here, and nothing that could run UI code happens.
     */
    void accept(SDL_Event event) {
        switch (event.type()) {
            case SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP -> {
                var key = event.key();
                var action = !key.down() ? InputConstants.RELEASE : key.repeat() ? InputConstants.REPEAT : InputConstants.PRESS;
                enqueue(new OsWindowEvent.Key(key.scancode(), key.key(), action, key.mod() & 0xFFFF));
            }
            case SDLEvents.SDL_EVENT_TEXT_INPUT -> {
                var text = event.text().textString();
                if (text != null && !text.isEmpty()) {
                    enqueue(new OsWindowEvent.Text(text));
                }
            }
            case SDLEvents.SDL_EVENT_TEXT_EDITING -> {
                var edit = event.edit();
                enqueue(new OsWindowEvent.Preedit(PreeditEvent.fromSdlTextEditing(edit.textString(), edit.start(), edit.length())));
            }
            case SDLEvents.SDL_EVENT_MOUSE_MOTION -> {
                var motion = event.motion();
                cursorX = motion.x();
                cursorY = motion.y();
                enqueueCursorPos(cursorX, cursorY);
            }
            case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN, SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP -> {
                var button = event.button();
                var index = button.button() & 0xFF;
                cursorX = button.x();
                cursorY = button.y();
                if (button.down()) {
                    heldButtons |= 1 << index;
                } else {
                    heldButtons &= ~(1 << index);
                }
                enqueue(new OsWindowEvent.MouseButton(index, button.down() ? InputConstants.PRESS : InputConstants.RELEASE,
                        SDLKeyboard.SDL_GetModState() & 0xFFFF));
            }
            case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> {
                var wheel = event.wheel();
                double scrollX = wheel.x();
                double scrollY = wheel.y();
                // The same platform convention vanilla's own handler applies: on macOS a shift-scroll
                // arrives as horizontal, and means vertical.
                if (InputQuirks.SHIFT_INVERTS_SCROLL_AXIS && scrollY == 0 && scrollX != 0
                        && (SDLKeyboard.SDL_GetModState() & InputConstants.MOD_SHIFT) != 0) {
                    scrollY = -scrollX;
                    scrollX = 0;
                }
                enqueue(new OsWindowEvent.Scroll(scrollX, scrollY));
            }
            case SDLEvents.SDL_EVENT_WINDOW_MOUSE_ENTER -> {
                // Another window may have changed the process-wide cursor in the meantime.
                currentCursorShape = -1;
                enqueue(new OsWindowEvent.CursorEnter(true));
            }
            case SDLEvents.SDL_EVENT_WINDOW_MOUSE_LEAVE -> enqueue(new OsWindowEvent.CursorEnter(false));
            case SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {
                framebufferWidth = event.window().data1();
                framebufferHeight = event.window().data2();
                enqueue(new OsWindowEvent.FramebufferSize(framebufferWidth, framebufferHeight));
            }
            case SDLEvents.SDL_EVENT_WINDOW_RESIZED -> {
                windowWidth = event.window().data1();
                windowHeight = event.window().data2();
            }
            case SDLEvents.SDL_EVENT_WINDOW_MOVED -> {
                positionX = event.window().data1();
                positionY = event.window().data2();
                enqueue(new OsWindowEvent.WindowPos(positionX, positionY));
            }
            case SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED, SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST -> {
                focused = event.type() == SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED;
                if (!focused) {
                    // A release that happens elsewhere is never reported here.
                    heldButtons = 0;
                }
                enqueue(new OsWindowEvent.Focus(focused));
            }
            case SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED -> enqueue(new OsWindowEvent.CloseRequest());
            case SDLEvents.SDL_EVENT_DROP_BEGIN -> droppingFiles.clear();
            case SDLEvents.SDL_EVENT_DROP_FILE -> {
                var path = event.drop().dataString();
                if (path != null) {
                    droppingFiles.add(new File(path));
                }
            }
            case SDLEvents.SDL_EVENT_DROP_COMPLETE -> {
                if (!droppingFiles.isEmpty()) {
                    var drop = event.drop();
                    enqueue(new OsWindowEvent.FileDrop(List.copyOf(droppingFiles), drop.x(), drop.y()));
                }
                droppingFiles.clear();
            }
            default -> {
                // Shown, hidden, exposed, occluded and the rest: nothing downstream reacts to them,
                // and they are still ours to swallow — see OsWindowEvents.
            }
        }
    }

    /**
     * Whether anything is waiting to be drained. Lets a host skip setting up its dispatch scopes on a
     * frame where nothing happened, which is most of them.
     */
    public boolean hasPendingEvents() {
        synchronized (pending) {
            return !pending.isEmpty();
        }
    }

    /**
     * Queues an event as though the platform had reported it.
     *
     * <p>For synthetic input — a test driving a window it cannot physically click, or a scripted
     * walkthrough. It goes through the same queue as a real event, so the host cannot tell the
     * difference and the whole dispatch path is exercised rather than bypassed.
     */
    public void post(OsWindowEvent event) {
        // A cursor move and a button update the cached state as well as queueing, exactly as a real
        // event does. Without that, a posted click - which re-reads the cursor rather than trusting the
        // last event, see ModularUIWindow#handleEvent - would land wherever the physical pointer
        // happens to be rather than where the caller just moved to.
        if (event instanceof OsWindowEvent.CursorPos cursor) {
            cursorX = cursor.x();
            cursorY = cursor.y();
            enqueueCursorPos(cursor.x(), cursor.y());
            return;
        }
        if (event instanceof OsWindowEvent.MouseButton button) {
            if (button.action() == InputConstants.PRESS) {
                heldButtons |= 1 << button.button();
            } else {
                heldButtons &= ~(1 << button.button());
            }
        }
        enqueue(event);
    }

    private void enqueue(OsWindowEvent event) {
        if (destroyed) return;
        synchronized (pending) {
            pending.add(event);
        }
    }

    private void enqueueCursorPos(double x, double y) {
        if (destroyed) return;
        synchronized (pending) {
            if (!pending.isEmpty() && pending.top() instanceof OsWindowEvent.CursorPos) {
                pending.set(pending.size() - 1, new OsWindowEvent.CursorPos(x, y));
                return;
            }
            pending.add(new OsWindowEvent.CursorPos(x, y));
        }
    }

    /**
     * Hands every queued event to {@code sink} and empties the queue.
     *
     * <p>The queue is drained into a copy first: dispatching UI events can open a dialog, close this
     * very window, or otherwise run code that enqueues more, and those belong to the next frame.
     */
    public void drain(Consumer<OsWindowEvent> sink) {
        OsWindowEvent[] batch;
        synchronized (pending) {
            if (pending.isEmpty()) return;
            batch = pending.toArray(new OsWindowEvent[0]);
            pending.clear();
        }
        for (var event : batch) {
            if (destroyed) return;
            sink.accept(event);
        }
    }

    // ------------------------------------------------------------------------------- queries

    /**
     * Whether {@code keyCode}, an {@code InputConstants.KEY_*} scancode, is held. SDL keeps one keyboard
     * state for whichever window has focus, so this is the same answer the game window would give; it
     * is only meaningful while this window is the focused one.
     */
    public boolean isKeyDown(int keyCode) {
        return !destroyed && focused && InputConstants.isKeyDown(keyCode);
    }

    /**
     * @param button an {@code InputConstants.MOUSE_BUTTON_*} value
     */
    public boolean isMouseButtonDown(int button) {
        return !destroyed && (heldButtons & (1 << button)) != 0;
    }

    /**
     * Cursor position in virtual-screen coordinates, queried live rather than taken from the last
     * event.
     *
     * <p>Dragging the window by its own title bar has to work in a frame of reference the drag does
     * not move. Window-relative coordinates are not one: as the window follows the cursor, the
     * cursor's position <em>within</em> it barely changes, so a delta computed from them collapses to
     * zero. The global position is that fixed frame.
     */
    public double[] queryGlobalCursor() {
        if (destroyed) return new double[]{0, 0};
        try (var stack = MemoryStack.stackPush()) {
            var x = stack.mallocFloat(1);
            var y = stack.mallocFloat(1);
            SDLMouse.SDL_GetGlobalMouseState(x, y);
            return new double[]{x.get(0), y.get(0)};
        }
    }

    public boolean isIconified() {
        return !destroyed && (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_MINIMIZED) != 0;
    }

    public boolean isMaximized() {
        return !destroyed && (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_MAXIMIZED) != 0;
    }

    public boolean isAlwaysOnTop() {
        return !destroyed && (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_ALWAYS_ON_TOP) != 0;
    }

    /**
     * Whether this platform lets a window pin itself above the others.
     *
     * <p>Wayland does not, on principle: a client there cannot raise or stack its own windows. Callers
     * should hide the control rather than offer one that silently does nothing.
     */
    public static boolean supportsAlwaysOnTop() {
        return !"wayland".equals(SDLVideo.SDL_GetCurrentVideoDriver());
    }

    // ------------------------------------------------------------------------------- commands

    public void setTitle(String title) {
        if (destroyed) return;
        SDLVideo.SDL_SetWindowTitle(handle, title);
    }

    /**
     * Moves the window. A no-op under Wayland, which does not let a client position its own windows —
     * there is no way around that, so a saved position simply does not survive there.
     */
    public void setPosition(int x, int y) {
        if (destroyed) return;
        SDLVideo.SDL_SetWindowPosition(handle, x, y);
    }

    public void setSize(int width, int height) {
        if (destroyed) return;
        SDLVideo.SDL_SetWindowSize(handle, Math.max(1, width), Math.max(1, height));
    }

    public void show() {
        if (destroyed) return;
        SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_SHOWN, activateOnShow ? "1" : "0");
        try {
            SDLVideo.SDL_ShowWindow(handle);
        } finally {
            SDLHints.SDL_ResetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_SHOWN);
        }
    }

    public void hide() {
        if (destroyed) return;
        SDLVideo.SDL_HideWindow(handle);
    }

    public void focus() {
        if (destroyed) return;
        SDLVideo.SDL_RaiseWindow(handle);
    }

    /**
     * Keeps the window above every other window, including the game's own.
     *
     * <p>The reason a second window is worth having at all is that it can sit beside the UI it is
     * about; on a single monitor, "beside" means "on top", because the game window is normally
     * maximised underneath.
     */
    public void setAlwaysOnTop(boolean onTop) {
        if (destroyed || !supportsAlwaysOnTop()) return;
        SDLVideo.SDL_SetWindowAlwaysOnTop(handle, onTop);
    }

    /**
     * Fills the monitor's work area. Works on an undecorated window too — the platform's window
     * manager handles it, so this behaves natively on each of them rather than guessing at monitor
     * bounds ourselves.
     */
    public void maximize() {
        if (destroyed) return;
        SDLVideo.SDL_MaximizeWindow(handle);
    }

    public void restore() {
        if (destroyed) return;
        SDLVideo.SDL_RestoreWindow(handle);
    }

    /**
     * Sets the pointer shape from an {@code SDL_SYSTEM_CURSOR_*} constant.
     *
     * <p>Without this an undecorated window gives no hint that its edges can be grabbed — the pointer
     * stays an arrow right up to the pixel where a drag would start, so the resize band may as well
     * not exist. SDL has one cursor for the whole process, shown in whichever window the pointer is
     * over, so it is only changed while the pointer is over this one; a shape the platform does not
     * provide is skipped, leaving the previous shape rather than blanking the pointer.
     */
    public void setCursorShape(int shape) {
        if (destroyed || shape == currentCursorShape) return;
        if (SDLMouse.SDL_GetMouseFocus() != handle) return;
        var cursor = CURSORS.computeIfAbsent(shape, SDLMouse::SDL_CreateSystemCursor);
        if (cursor == MemoryUtil.NULL) return;
        currentCursorShape = shape;
        SDLMouse.SDL_SetCursor(cursor);
    }

    // ------------------------------------------------------------------------------- text input

    /**
     * Starts delivering {@link OsWindowEvent.Text} and {@link OsWindowEvent.Preedit} for this window.
     * SDL only turns key presses into text — and only runs the input method — while this is on, so a
     * text field in this window calls it when it gains focus.
     */
    public void startTextInput() {
        if (destroyed || textInputStarted) return;
        textInputStarted = true;
        SDLKeyboard.SDL_StartTextInput(handle);
        SDLKeyboard.SDL_ClearComposition(handle);
    }

    public void stopTextInput() {
        if (destroyed || !textInputStarted) return;
        textInputStarted = false;
        SDLKeyboard.SDL_StopTextInput(handle);
    }

    public boolean isTextInputStarted() {
        return textInputStarted;
    }

    /**
     * Where the text being edited is, in window coordinates, so the input method places its candidate
     * list next to it instead of in a corner of the screen.
     */
    public void setTextInputArea(int x, int y, int width, int height) {
        if (destroyed) return;
        try (var stack = MemoryStack.stackPush()) {
            var rect = SDL_Rect.malloc(1, stack).x(x).y(y).w(Math.max(1, width)).h(Math.max(1, height));
            SDLKeyboard.SDL_SetTextInputArea(handle, rect, -1);
        }
    }

    // ------------------------------------------------------------------------------- lifetime

    /**
     * Destroys the window. Idempotent, because this can be reached both from a close request and from
     * the game shutting down.
     */
    public void destroy() {
        if (destroyed) return;
        stopTextInput();
        destroyed = true;
        synchronized (pending) {
            pending.clear();
        }
        // Destroyed first, retired after: destroying queues a focus loss and friends for this window,
        // and retiring is what keeps those from reaching the game's.
        SDLVideo.SDL_DestroyWindow(handle);
        OsWindowEvents.retire(this);
    }
}
