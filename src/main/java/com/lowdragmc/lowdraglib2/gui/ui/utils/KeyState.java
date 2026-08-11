package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.utils.Scope;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Physical keyboard state, as seen by UI code that needs to know whether a modifier is held while
 * something else happens — shift-clicking a slot, ctrl-dragging a gizmo, shift-scrolling a number
 * field.
 *
 * <p>This exists as an indirection rather than a direct {@code InputConstants} call because GLFW
 * reports the <em>real</em> keyboard and there is no way to inject a key press into it from inside
 * the process. Neither dispatching a synthetic {@code keyPressed} nor calling Minecraft's own
 * {@code KeyboardHandler} changes what {@code glfwGetKey} returns. Without a seam here, every
 * modifier-sensitive behaviour in the library is simply untestable.
 *
 * <p>The seam also matters at runtime, not only under test: GLFW key state is per-window, so a UI
 * hosted in its own operating-system window has to read <em>that</em> window's keyboard. With the
 * game window's state, every shortcut in the library quietly returns false while the second window
 * is focused.
 *
 * <p>Everything that actually touches Minecraft lives in {@link KeyStateClientAccess}, so this class
 * stays loadable on a dedicated server. With no source installed and no client, every query is
 * false.
 *
 * @see #setSource(Source)
 */
public final class KeyState {

    /** Where held-key state comes from. */
    @FunctionalInterface
    public interface Source {
        boolean isKeyDown(int keyCode);
    }

    @Nullable
    private static Source source;

    private KeyState() {
    }

    /**
     * Overrides where key state is read from, or restores the GLFW default with {@code null}.
     *
     * <p>Intended for automated tests and scripted playback. Anything that sets this <b>must</b>
     * clear it again, including on failure — a leaked override makes the real keyboard stop working.
     * Prefer {@link #clearSource(Source)} for that, so overlapping owners cannot clear each other's.
     */
    public static void setSource(@Nullable Source source) {
        KeyState.source = source;
    }

    /**
     * Clears the override only if {@code owner} is the one currently installed.
     *
     * <p>Two things can legitimately want this at once — a scripted playback and an interactive test
     * run, say. Last writer wins, which is fine; what is not fine is the loser's teardown clearing
     * the winner's override, because the winner then silently starts reading the real keyboard and
     * its modifier-dependent behaviour quietly stops working rather than failing.
     */
    public static void clearSource(Source owner) {
        if (source == owner) {
            source = null;
        }
    }

    /**
     * Installs {@code source} until the returned scope is closed, restoring whatever was installed
     * before rather than clearing.
     *
     * <p>{@link #setSource(Source)} is for an override that lives for a whole run — a scripted
     * playback. This is for one that lives for a single dispatch, such as a UI hosted in its own OS
     * window reading that window's keyboard while it handles its own events. The two nest: outside
     * the scope the long-lived override is still in force.
     */
    public static Scope scoped(Source source) {
        var previous = KeyState.source;
        KeyState.source = source;
        return () -> KeyState.source = previous;
    }

    public static boolean isKeyDown(int keyCode) {
        var current = source;
        if (current != null) return current.isKeyDown(keyCode);
        return KeyStateClientAccess.isKeyDown(keyCode);
    }

    public static boolean isShiftDown() {
        var current = source;
        if (current != null) {
            return current.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || current.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
        }
        return KeyStateClientAccess.isShiftDown();
    }

    /**
     * The control key itself, on every platform. {@link #isCtrlOrCmdDown()} is what a keyboard
     * shortcut should be testing.
     */
    public static boolean isCtrlDown() {
        var current = source;
        if (current != null) {
            return current.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || current.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
        }
        return KeyStateClientAccess.isCtrlDown();
    }

    /**
     * The platform's primary shortcut modifier — command on a Mac keyboard, control everywhere else.
     *
     * <p>Under an installed {@link Source} either key counts, rather than only the one the running
     * platform would use. A test that presses control is asking for the shortcut, and making it
     * depend on which machine the suite happens to run on would buy nothing.
     */
    public static boolean isCtrlOrCmdDown() {
        var current = source;
        if (current != null) {
            return isCtrlDown()
                    || current.isKeyDown(GLFW.GLFW_KEY_LEFT_SUPER)
                    || current.isKeyDown(GLFW.GLFW_KEY_RIGHT_SUPER);
        }
        return KeyStateClientAccess.isCtrlOrCmdDown();
    }

    public static boolean isAltDown() {
        var current = source;
        if (current != null) {
            return current.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || current.isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT);
        }
        return KeyStateClientAccess.isAltDown();
    }

}
