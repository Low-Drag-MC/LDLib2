package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.utils.Scope;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
 * @see #setSource(Source)
 */
@OnlyIn(Dist.CLIENT)
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

    @Nullable
    public static Source getSource() {
        return source;
    }

    public static boolean isKeyDown(int keyCode) {
        var current = source;
        if (current != null) return current.isKeyDown(keyCode);
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyCode);
    }

    public static boolean isShiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isCtrlDown() {
        var current = source;
        if (current != null) {
            // Mirrors Screen#hasControlDown: on macOS the command key plays the role of control.
            if (Minecraft.ON_OSX) {
                return current.isKeyDown(GLFW.GLFW_KEY_LEFT_SUPER) || current.isKeyDown(GLFW.GLFW_KEY_RIGHT_SUPER);
            }
            return current.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || current.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
        }
        return Screen.hasControlDown();
    }

    public static boolean isAltDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT);
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

    // Clipboard and selection chords. Screen#isCopy and friends read the *main* window handle
    // directly, so with any other window focused they all quietly return false and every shortcut in
    // the library stops working. These are the same chords, resolved through this class instead.

    public static boolean isCut(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_X && isCommandChord();
    }

    public static boolean isPaste(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_V && isCommandChord();
    }

    public static boolean isCopy(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_C && isCommandChord();
    }

    public static boolean isSelectAll(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_A && isCommandChord();
    }

    /**
     * Control (command on macOS) held, with neither shift nor alt — the modifier state every
     * single-key editor command requires.
     */
    public static boolean isCommandChord() {
        return isCtrlDown() && !isShiftDown() && !isAltDown();
    }
}
