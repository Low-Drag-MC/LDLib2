package com.lowdragmc.lowdraglib2.uitest.input;

import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLKeycode;

/**
 * Keyboard helpers for scenarios: key constants are re-exported so a scenario does not have to
 * import LWJGL, and keys are mapped to the {@code (key, keycode, modifiers)} triple that
 * {@link net.minecraft.client.gui.screens.Screen#keyPressed} expects.
 *
 * <p>Keys are SDL scancodes — the physical key, the values vanilla's own input records carry. Mouse
 * buttons are LDLib2's own numbering, {@code UIEvent.BUTTON_*} — left 0, right 1, middle 2 — the same
 * numbers a UI handler compares {@code event.button} against; the drivers convert to SDL's numbering
 * where they hand input to the game, as the real input path does.
 */
public final class Keys {

    public static final int ENTER = InputConstants.KEY_RETURN;
    public static final int ESCAPE = InputConstants.KEY_ESCAPE;
    public static final int TAB = InputConstants.KEY_TAB;
    public static final int BACKSPACE = InputConstants.KEY_BACKSPACE;
    public static final int DELETE = InputConstants.KEY_DELETE;
    public static final int UP = InputConstants.KEY_UP;
    public static final int DOWN = InputConstants.KEY_DOWN;
    public static final int LEFT = InputConstants.KEY_LEFT;
    public static final int RIGHT = InputConstants.KEY_RIGHT;
    public static final int HOME = InputConstants.KEY_HOME;
    public static final int END = InputConstants.KEY_END;
    public static final int SPACE = InputConstants.KEY_SPACE;

    public static final int LEFT_SHIFT = InputConstants.KEY_LSHIFT;
    public static final int LEFT_CONTROL = InputConstants.KEY_LCONTROL;
    public static final int LEFT_ALT = InputConstants.KEY_LALT;

    public static final int MOD_SHIFT = InputConstants.MOD_SHIFT;
    public static final int MOD_CONTROL = InputConstants.MOD_CONTROL;
    public static final int MOD_ALT = InputConstants.MOD_ALT;

    public static final int MOUSE_LEFT = UIEvent.BUTTON_LEFT;
    public static final int MOUSE_RIGHT = UIEvent.BUTTON_RIGHT;
    public static final int MOUSE_MIDDLE = UIEvent.BUTTON_MIDDLE;

    private Keys() {
    }

    /**
     * The keycode a physical key produces — what the key means, which shortcuts such as
     * {@code KeyEvent#isCopy} match on.
     *
     * <p>Deliberately the US layout rather than whatever the machine running the suite has: a
     * scenario that presses the Z key and expects an undo must not become a redo on a German keyboard,
     * where that key is Y.
     */
    public static int keycodeOf(int keyCode) {
        if (keyCode >= InputConstants.KEY_A && keyCode <= InputConstants.KEY_Z) {
            return SDLKeycode.SDLK_A + keyCode - InputConstants.KEY_A;
        }
        if (keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_9) {
            return SDLKeycode.SDLK_1 + keyCode - InputConstants.KEY_1;
        }
        return switch (keyCode) {
            case InputConstants.KEY_0 -> SDLKeycode.SDLK_0;
            case InputConstants.KEY_RETURN -> SDLKeycode.SDLK_RETURN;
            case InputConstants.KEY_ESCAPE -> SDLKeycode.SDLK_ESCAPE;
            case InputConstants.KEY_BACKSPACE -> SDLKeycode.SDLK_BACKSPACE;
            case InputConstants.KEY_TAB -> SDLKeycode.SDLK_TAB;
            case InputConstants.KEY_SPACE -> SDLKeycode.SDLK_SPACE;
            case InputConstants.KEY_MINUS -> SDLKeycode.SDLK_MINUS;
            case InputConstants.KEY_EQUALS -> SDLKeycode.SDLK_EQUALS;
            case InputConstants.KEY_LBRACKET -> SDLKeycode.SDLK_LEFTBRACKET;
            case InputConstants.KEY_RBRACKET -> SDLKeycode.SDLK_RIGHTBRACKET;
            case InputConstants.KEY_BACKSLASH -> SDLKeycode.SDLK_BACKSLASH;
            case InputConstants.KEY_SEMICOLON -> SDLKeycode.SDLK_SEMICOLON;
            case InputConstants.KEY_APOSTROPHE -> SDLKeycode.SDLK_APOSTROPHE;
            case InputConstants.KEY_GRAVE -> SDLKeycode.SDLK_GRAVE;
            case InputConstants.KEY_COMMA -> SDLKeycode.SDLK_COMMA;
            case InputConstants.KEY_PERIOD -> SDLKeycode.SDLK_PERIOD;
            case InputConstants.KEY_SLASH -> SDLKeycode.SDLK_SLASH;
            case InputConstants.KEY_DELETE -> SDLKeycode.SDLK_DELETE;
            // Every other key is named by its scancode, the rule SDL itself follows for keys that
            // type nothing — arrows, function keys, the keypad and the modifiers.
            default -> keyCode | SDLKeycode.SDLK_SCANCODE_MASK;
        };
    }

    /**
     * Whether the character can be delivered as a {@code charTyped} event. Control characters cannot;
     * they have to go through {@code keyPressed} instead.
     */
    public static boolean isPrintable(char c) {
        return c >= 32 && c != 127;
    }

    /**
     * The physical keys a modifier mask implies, so a harness can hold them rather than only
     * announcing them in an event. Nothing reads the mask when asking "is ctrl down right now".
     */
    public static java.util.List<Integer> modifierKeysOf(int modifiers) {
        if (modifiers == 0) return java.util.List.of();
        var keys = new java.util.ArrayList<Integer>(3);
        if ((modifiers & MOD_CONTROL) != 0) keys.add(InputConstants.KEY_LCONTROL);
        if ((modifiers & MOD_SHIFT) != 0) keys.add(InputConstants.KEY_LSHIFT);
        if ((modifiers & MOD_ALT) != 0) keys.add(InputConstants.KEY_LALT);
        return keys;
    }

    /** A readable name for a key code, for step labels and report entries. */
    public static String nameOf(int keyCode) {
        try {
            return InputConstants.Type.KEYBOARD.getOrCreate(keyCode).getName();
        } catch (Exception e) {
            return "key:" + keyCode;
        }
    }

    public static String describe(int keyCode, int modifiers) {
        var name = nameOf(keyCode);
        if (modifiers == 0) return name;
        var prefix = new StringBuilder();
        if ((modifiers & MOD_CONTROL) != 0) prefix.append("ctrl+");
        if ((modifiers & MOD_SHIFT) != 0) prefix.append("shift+");
        if ((modifiers & MOD_ALT) != 0) prefix.append("alt+");
        return prefix + name;
    }
}
