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

    // ── Modifiers carried by an event, rather than polled ────────────────────────────────────────
    //
    // Preferred over the polling methods above wherever the modifiers of one specific key press are
    // what matters — resolving a shortcut, capturing a chord. The bits come from the GLFW callback of
    // whichever window the press arrived at, so they are right in a torn-off window without a scoped
    // Source, they are right for the key being pressed at the moment it is pressed, and a test driver
    // can supply them outright. This is the same thing the game's own InputWithModifiers#hasControlDown
    // and friends read, which is why KeyEvent#isCopy works in a second window.

    public static boolean isShiftDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    public static boolean isAltDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

    /**
     * Whether the event's modifiers say the platform's primary shortcut modifier was held — command
     * on a Mac keyboard, control everywhere else.
     *
     * <p>Under an installed {@link Source} either bit counts, for the same reason
     * {@link #isCtrlOrCmdDown()} accepts either key: a test that presses control is asking for the
     * shortcut, and making that depend on which machine the suite runs on would buy nothing.
     */
    public static boolean isCtrlOrCmdDown(int modifiers) {
        var current = source;
        if (current != null) {
            return (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        }
        return (modifiers & KeyStateClientAccess.shortcutModifierBit()) != 0;
    }

    /**
     * The modifier bit a key press of its own implies, or zero for a key that is not a modifier.
     *
     * <p>GLFW is not consistent across platforms about whether a modifier's own press event carries
     * its bit, and a chord capture field shows "Ctrl+…" the moment control goes down — so the bit is
     * put in explicitly rather than trusted to be there.
     */
    public static int modifierBitOf(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> GLFW.GLFW_MOD_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> GLFW.GLFW_MOD_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> GLFW.GLFW_MOD_ALT;
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> GLFW.GLFW_MOD_SUPER;
            default -> 0;
        };
    }

    /**
     * Whether this key will put a character into a focused text field — space, the ASCII punctuation
     * and letter block, and the numeric keypad.
     *
     * <p>⚠️ It matters at {@code KEY_DOWN} time even though the character itself arrives as
     * {@code CHAR_TYPED}: an editable field that lets such a key bubble has the character typed
     * <i>and</i> whatever shortcut an ancestor hangs off that key fired. Space is the one that bites —
     * it is the play/pause key of every timeline and it appears in no field's own key switch.
     *
     * <p>GLFW numbers the printable block contiguously from {@code APOSTROPHE} (39) to
     * {@code GRAVE_ACCENT} (96) — the digits, the letters, the brackets and the punctuation; space
     * sits at 32 on its own and the keypad at 320–336.
     */
    public static boolean isTextKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_SPACE
                || (keyCode >= GLFW.GLFW_KEY_APOSTROPHE && keyCode <= GLFW.GLFW_KEY_GRAVE_ACCENT)
                || (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_EQUAL);
    }
}
