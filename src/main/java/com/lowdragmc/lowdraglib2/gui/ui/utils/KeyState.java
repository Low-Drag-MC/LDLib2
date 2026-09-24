package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.utils.Scope;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;

/**
 * Physical keyboard state, as seen by UI code that needs to know whether a modifier is held while
 * something else happens — shift-clicking a slot, ctrl-dragging a gizmo, shift-scrolling a number
 * field.
 *
 * <p>Key codes here are SDL scancodes — the {@code InputConstants.KEY_*} values, which name a key by
 * where it sits on a US keyboard rather than by what it types. That is the right unit for "is this
 * held": {@code SDL_GetKeyboardState} is indexed by scancode.
 *
 * <p>This exists as an indirection rather than a direct {@code InputConstants} call because the
 * platform reports the <em>real</em> keyboard and there is no way to inject a key press into it from
 * inside the process. Neither dispatching a synthetic {@code keyPressed} nor calling Minecraft's own
 * {@code KeyboardHandler} changes what {@code SDL_GetKeyboardState} returns. Without a seam here,
 * every modifier-sensitive behaviour in the library is simply untestable.
 *
 * <p>SDL keeps one keyboard state for whichever window has keyboard focus, so a UI hosted in its own
 * operating-system window reads the same state as the game's — unlike GLFW, which kept it per window.
 *
 * <p>Everything that actually touches Minecraft lives in {@link KeyStateClientAccess}, so this class
 * stays loadable on a dedicated server. Every {@code InputConstants} reference below is a compile-time
 * constant and is inlined, so the client-only class is never loaded from here. With no source
 * installed and no client, every query is false.
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
     * Overrides where key state is read from, or restores the real keyboard with {@code null}.
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
     * playback. This is for one that lives for a single dispatch. The two nest: outside the scope the
     * long-lived override is still in force.
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
            return current.isKeyDown(InputConstants.KEY_LSHIFT) || current.isKeyDown(InputConstants.KEY_RSHIFT);
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
            return current.isKeyDown(InputConstants.KEY_LCONTROL) || current.isKeyDown(InputConstants.KEY_RCONTROL);
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
                    || current.isKeyDown(InputConstants.KEY_LGUI)
                    || current.isKeyDown(InputConstants.KEY_RGUI);
        }
        return KeyStateClientAccess.isCtrlOrCmdDown();
    }

    public static boolean isAltDown() {
        var current = source;
        if (current != null) {
            return current.isKeyDown(InputConstants.KEY_LALT) || current.isKeyDown(InputConstants.KEY_RALT);
        }
        return KeyStateClientAccess.isAltDown();
    }

    // ── Modifiers carried by an event, rather than polled ────────────────────────────────────────
    //
    // Preferred over the polling methods above wherever the modifiers of one specific key press are
    // what matters — resolving a shortcut, capturing a chord. The bits are the SDL_KMOD_* mask the
    // event was delivered with, so they are right for the key being pressed at the moment it is
    // pressed, and a test driver can supply them outright. This is the same thing the game's own
    // InputWithModifiers#hasControlDown and friends read.

    public static boolean isShiftDown(int modifiers) {
        return (modifiers & InputConstants.MOD_SHIFT) != 0;
    }

    public static boolean isAltDown(int modifiers) {
        return (modifiers & InputConstants.MOD_ALT) != 0;
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
            return (modifiers & (InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER)) != 0;
        }
        return (modifiers & KeyStateClientAccess.shortcutModifierBit()) != 0;
    }

    /**
     * The modifier bits a key press of its own implies, or zero for a key that is not a modifier.
     *
     * <p>A chord capture field shows "Ctrl+…" the moment control goes down, and whether a modifier's
     * own press event already carries its bit differs between platforms — so the bit is put in
     * explicitly rather than trusted to be there.
     */
    public static int modifierBitOf(int keyCode) {
        return switch (keyCode) {
            case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> InputConstants.MOD_SHIFT;
            case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> InputConstants.MOD_CONTROL;
            case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> InputConstants.MOD_ALT;
            case InputConstants.KEY_LGUI, InputConstants.KEY_RGUI -> InputConstants.MOD_SUPER;
            default -> 0;
        };
    }

    /**
     * Whether this key will put a character into a focused text field — space, the letter, digit and
     * punctuation keys, and the numeric keypad bar its Enter.
     *
     * <p>⚠️ It matters at {@code KEY_DOWN} time even though the character itself arrives as
     * {@code CHAR_TYPED}: an editable field that lets such a key bubble has the character typed
     * <i>and</i> whatever shortcut an ancestor hangs off that key fired. Space is the one that bites —
     * it is the play/pause key of every timeline and it appears in no field's own key switch.
     *
     * <p>SDL numbers the letters (4–29) and digits (30–39) contiguously; Enter, Escape, Backspace and
     * Tab (40–43) come next and are not text, then space and the punctuation block (44–56). The keypad
     * runs 84–99 with Enter (88) in the middle, plus keypad-equals at 103, and the ISO key left of Z
     * sits alone at 100.
     */
    public static boolean isTextKey(int keyCode) {
        return (keyCode >= InputConstants.KEY_A && keyCode <= InputConstants.KEY_0)
                || (keyCode >= InputConstants.KEY_SPACE && keyCode <= InputConstants.KEY_SLASH)
                || (keyCode >= 84 && keyCode <= 99 && keyCode != InputConstants.KEY_NUMPADENTER)
                || keyCode == 100
                || keyCode == InputConstants.KEY_NUMPADEQUALS;
    }
}
