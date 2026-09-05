package com.lowdragmc.lowdraglib2.client.window;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A second operating-system window, alongside the game's own.
 *
 * <p>Owns the GLFW handle, its callbacks and its event queue, and nothing else — no OpenGL, no UI.
 * Presenting is {@link OsWindowPresenter}; what goes in it is an {@link OsWindowHost}.
 *
 * <p>Everything here runs on the render thread, which is also the process main thread (Minecraft
 * names it "Render thread" but it is the one the JVM started on, and on macOS that is the only
 * thread GLFW will accept window calls from). That is what makes creating and destroying windows
 * from a frame hook legal on all three platforms.
 */
public final class OsWindow {

    /**
     * Standard cursors, shared across every window — GLFW cursors are not owned by one — and created
     * lazily because most windows only ever need the arrow.
     */
    private static final Map<Integer, Long> CURSORS = new HashMap<>();

    private final long handle;
    private final ObjectArrayList<OsWindowEvent> pending = new ObjectArrayList<>();
    private int currentCursorShape = GLFW.GLFW_ARROW_CURSOR;

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
    }

    /**
     * Creates a window the game can present into.
     *
     * <p>Under OpenGL its context shares textures, buffers and programs with the game's. Under Vulkan
     * it carries no context at all and there is nothing to share — the frame reaches it through a
     * swapchain instead, see {@link OsWindowBackend}.
     *
     * @param decorated whether the OS draws the frame, see
     *                  {@link OsWindowHints#applyChildWindowHints(boolean)}
     * @return the window, or {@code null} if the platform refused — a headless or null-platform GLFW,
     *         a compositor that will not give us a second context, or an exhausted driver. Callers
     *         are expected to fall back to an in-game panel rather than treat this as fatal.
     */
    @Nullable
    public static OsWindow create(String title, int width, int height, boolean decorated) {
        RenderSystem.assertOnRenderThread();
        if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_NULL) {
            LDLib2.LOGGER.warn("[os-window] GLFW has no windowing platform, cannot open '{}'", title);
            return null;
        }
        // MemoryUtil.NULL under Vulkan: glfwCreateWindow rejects a share window when the client API
        // is GLFW_NO_API, so passing the game's handle would fail the creation outright.
        var shareWith = OsWindowBackend.current().isOpenGl()
                ? Minecraft.getInstance().getWindow().handle()
                : MemoryUtil.NULL;

        // Minecraft installs an error callback that logs loudly and, off the render thread, throws.
        // A refused context is an expected outcome here, so collect the message instead of letting it
        // look like a crash.
        var errors = new ArrayList<String>();
        var previousErrorCallback = GLFW.glfwSetErrorCallback(
                GLFWErrorCallback.create((error, description) ->
                        errors.add(error + ": " + GLFWErrorCallback.getDescription(description))));
        long handle;
        try {
            OsWindowHints.applyChildWindowHints(decorated);
            handle = GLFW.glfwCreateWindow(Math.max(1, width), Math.max(1, height), title,
                    MemoryUtil.NULL, shareWith);
        } finally {
            var installed = GLFW.glfwSetErrorCallback(previousErrorCallback);
            if (installed != null) {
                installed.free();
            }
            // Leave the hints as Minecraft would expect to find them.
            GLFW.glfwDefaultWindowHints();
        }

        if (handle == MemoryUtil.NULL) {
            LDLib2.LOGGER.warn("[os-window] could not create '{}' ({}); falling back to an in-game panel",
                    title, errors.isEmpty() ? "no GLFW error reported" : String.join("; ", errors));
            return null;
        }

        var window = new OsWindow(handle);
        window.installCallbacks();
        window.refreshGeometry();
        return window;
    }

    public long handle() {
        return handle;
    }

    private void installCallbacks() {
        GLFW.glfwSetWindowCloseCallback(handle, w -> {
            // Ignore GLFW's own close flag; the host decides, and it may want to prompt first.
            GLFW.glfwSetWindowShouldClose(w, false);
            enqueue(new OsWindowEvent.CloseRequest());
        });
        GLFW.glfwSetFramebufferSizeCallback(handle, (w, width, height) -> {
            framebufferWidth = width;
            framebufferHeight = height;
            enqueue(new OsWindowEvent.FramebufferSize(width, height));
        });
        GLFW.glfwSetWindowSizeCallback(handle, (w, width, height) -> {
            windowWidth = width;
            windowHeight = height;
        });
        GLFW.glfwSetWindowPosCallback(handle, (w, x, y) -> {
            positionX = x;
            positionY = y;
            enqueue(new OsWindowEvent.WindowPos(x, y));
        });
        GLFW.glfwSetWindowFocusCallback(handle, (w, isFocused) -> {
            focused = isFocused;
            enqueue(new OsWindowEvent.Focus(isFocused));
        });
        GLFW.glfwSetCursorPosCallback(handle, (w, x, y) -> {
            cursorX = x;
            cursorY = y;
            enqueueCursorPos(x, y);
        });
        GLFW.glfwSetCursorEnterCallback(handle, (w, entered) -> enqueue(new OsWindowEvent.CursorEnter(entered)));
        GLFW.glfwSetMouseButtonCallback(handle, (w, button, action, mods) ->
                enqueue(new OsWindowEvent.MouseButton(button, action, mods)));
        GLFW.glfwSetScrollCallback(handle, (w, dx, dy) -> enqueue(new OsWindowEvent.Scroll(dx, dy)));
        GLFW.glfwSetKeyCallback(handle, (w, key, scancode, action, mods) ->
                enqueue(new OsWindowEvent.Key(key, scancode, action, mods)));
        GLFW.glfwSetCharModsCallback(handle, (w, codepoint, mods) ->
                enqueue(new OsWindowEvent.Char(codepoint, mods)));
        GLFW.glfwSetDropCallback(handle, (w, count, names) -> {
            var files = new ArrayList<File>(count);
            for (int i = 0; i < count; i++) {
                files.add(new File(MemoryUtil.memUTF8(MemoryUtil.memGetAddress(names + (long) i * Long.BYTES))));
            }
            enqueue(new OsWindowEvent.FileDrop(List.copyOf(files)));
        });
    }

    private void refreshGeometry() {
        var buffer = new int[2];
        var second = new int[1];
        GLFW.glfwGetFramebufferSize(handle, buffer, second);
        framebufferWidth = buffer[0];
        framebufferHeight = second[0];
        GLFW.glfwGetWindowSize(handle, buffer, second);
        windowWidth = buffer[0];
        windowHeight = second[0];
        GLFW.glfwGetWindowPos(handle, buffer, second);
        positionX = buffer[0];
        positionY = second[0];
        focused = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    /**
     * Whether anything is waiting to be drained. Lets a host skip setting up its dispatch scopes on a
     * frame where nothing happened, which is most of them.
     */
    public boolean hasPendingEvents() {
        return !pending.isEmpty();
    }

    /**
     * Queues an event as though the platform had reported it.
     *
     * <p>For synthetic input — a test driving a window it cannot physically click, or a scripted
     * walkthrough. It goes through the same queue as a real callback, so the host cannot tell the
     * difference and the whole dispatch path is exercised rather than bypassed.
     */
    public void post(OsWindowEvent event) {
        // A cursor move updates the cached position as well as queueing, exactly as the platform's own
        // callback does. Without that, a posted click - which re-reads the cursor rather than trusting
        // the last event, see ModularUIWindow#handleEvent - would land wherever the physical pointer
        // happens to be rather than where the caller just moved to.
        if (event instanceof OsWindowEvent.CursorPos cursor) {
            if (destroyed) return;
            cursorX = cursor.x();
            cursorY = cursor.y();
            enqueueCursorPos(cursor.x(), cursor.y());
            return;
        }
        enqueue(event);
    }

    private void enqueue(OsWindowEvent event) {
        if (destroyed) return;
        pending.add(event);
    }

    private void enqueueCursorPos(double x, double y) {
        if (destroyed) return;
        if (!pending.isEmpty() && pending.top() instanceof OsWindowEvent.CursorPos) {
            pending.set(pending.size() - 1, new OsWindowEvent.CursorPos(x, y));
            return;
        }
        pending.add(new OsWindowEvent.CursorPos(x, y));
    }

    /**
     * Hands every queued event to {@code sink} and empties the queue.
     *
     * <p>The queue is drained into a copy first: dispatching UI events can open a dialog, close this
     * very window, or otherwise run code that enqueues more, and those belong to the next frame.
     */
    public void drain(Consumer<OsWindowEvent> sink) {
        if (pending.isEmpty()) return;
        var batch = pending.toArray(new OsWindowEvent[0]);
        pending.clear();
        for (var event : batch) {
            if (destroyed) return;
            sink.accept(event);
        }
    }

    /**
     * Whether {@code keyCode} is held <em>in this window</em>. GLFW key state is per-window, which is
     * exactly why a UI hosted here cannot use {@code Screen.hasControlDown}.
     */
    public boolean isKeyDown(int keyCode) {
        // Straight to GLFW rather than InputConstants, which only takes Minecraft's own Window —
        // and this window's keyboard is the whole point.
        return !destroyed && GLFW.glfwGetKey(handle, keyCode) != GLFW.GLFW_RELEASE;
    }

    public boolean isMouseButtonDown(int button) {
        return !destroyed && GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
    }

    public void setTitle(String title) {
        if (destroyed) return;
        GLFW.glfwSetWindowTitle(handle, title);
    }

    /**
     * Moves the window. A no-op under Wayland, which does not let a client position its own windows —
     * there is no way around that, so a saved position simply does not survive there.
     */
    public void setPosition(int x, int y) {
        if (destroyed) return;
        GLFW.glfwSetWindowPos(handle, x, y);
    }

    public void setSize(int width, int height) {
        if (destroyed) return;
        GLFW.glfwSetWindowSize(handle, Math.max(1, width), Math.max(1, height));
    }

    public void show() {
        if (destroyed) return;
        GLFW.glfwShowWindow(handle);
    }

    public void hide() {
        if (destroyed) return;
        GLFW.glfwHideWindow(handle);
    }

    public void focus() {
        if (destroyed) return;
        GLFW.glfwFocusWindow(handle);
    }

    public boolean isIconified() {
        return !destroyed && GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
    }

    /**
     * Whether this platform lets a window pin itself above the others.
     *
     * <p>Wayland does not, on principle: a client there cannot raise or stack its own windows, and
     * asking anyway is a {@code GLFW_FEATURE_UNAVAILABLE} that surfaces through Minecraft's error
     * callback as what looks like a crash. Callers should hide the control rather than offer one that
     * silently does nothing.
     */
    public static boolean supportsAlwaysOnTop() {
        return GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND;
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
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FLOATING, onTop ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    }

    public boolean isAlwaysOnTop() {
        return !destroyed && GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FLOATING) == GLFW.GLFW_TRUE;
    }

    public boolean isMaximized() {
        return !destroyed && GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
    }

    /**
     * Fills the monitor's work area. Works on an undecorated window too — the platform's window
     * manager handles it, so this behaves natively on each of them rather than guessing at monitor
     * bounds ourselves.
     */
    public void maximize() {
        if (destroyed) return;
        GLFW.glfwMaximizeWindow(handle);
    }

    public void restore() {
        if (destroyed) return;
        GLFW.glfwRestoreWindow(handle);
    }

    /**
     * Cursor position in virtual-screen coordinates, queried live rather than taken from the last
     * callback.
     *
     * <p>Dragging the window by its own title bar has to work in a frame of reference the drag does
     * not move. Window-relative coordinates are not one: as the window follows the cursor, the
     * cursor's position <em>within</em> it barely changes, so a delta computed from them collapses to
     * zero. Adding the live window position gives a fixed frame, and querying both at the same
     * instant avoids pairing a stale position with a fresh cursor.
     */
    public double[] queryGlobalCursor() {
        if (destroyed) return new double[]{0, 0};
        var cursorX = new double[1];
        var cursorY = new double[1];
        GLFW.glfwGetCursorPos(handle, cursorX, cursorY);
        var posX = new int[1];
        var posY = new int[1];
        GLFW.glfwGetWindowPos(handle, posX, posY);
        return new double[]{posX[0] + cursorX[0], posY[0] + cursorY[0]};
    }

    /**
     * Sets the pointer shape from a {@code GLFW_*_CURSOR} constant.
     *
     * <p>Without this an undecorated window gives no hint that its edges can be grabbed — the pointer
     * stays an arrow right up to the pixel where a drag would start, so the resize band may as well
     * not exist. Cursors are created once and cached; a shape the platform does not provide (the
     * diagonal ones need GLFW 3.4 and a cooperating desktop) resolves to null and is skipped, leaving
     * the previous shape rather than blanking the pointer.
     */
    public void setCursorShape(int shape) {
        if (destroyed || shape == currentCursorShape) return;
        var cursor = CURSORS.computeIfAbsent(shape, GLFW::glfwCreateStandardCursor);
        if (cursor == MemoryUtil.NULL && shape != GLFW.GLFW_ARROW_CURSOR) return;
        currentCursorShape = shape;
        GLFW.glfwSetCursor(handle, cursor);
    }

    /**
     * Frees the native callbacks and the window. Idempotent, because this can be reached both from a
     * close request and from the game shutting down.
     */
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        pending.clear();
        Callbacks.glfwFreeCallbacks(handle);
        GLFW.glfwDestroyWindow(handle);
    }
}
