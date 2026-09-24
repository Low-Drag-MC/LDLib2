package com.lowdragmc.lowdraglib2.editor.keymap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.sdl.SDLKeycode;

import java.util.Locale;

/**
 * The stable text names key codes are stored under, and the pretty ones they are shown under.
 *
 * <p>The codes are SDL keycodes — what a key <em>means</em> under the current layout, the same unit
 * vanilla's {@code KeyEvent#shortcutKey()} reports — so a chord named "ctrl+z" is the key labelled Z on
 * every layout. For a printable key that is simply its lower-case character.
 *
 * <p>Stored names are this library's own rather than raw numbers or Minecraft's {@code key.keyboard.*}
 * translation keys: a number in a config file means nothing to a human editing it, and it would also
 * tie a saved keymap to one windowing backend's numbering — which is exactly what survived the move
 * from GLFW to SDL: the names in a keymap written under GLFW read back unchanged.
 *
 * <p>A printable key outside the table below — {@code ö} on a German keyboard, say — is named by its
 * own character, so every key a layout has can be bound, not only the ones a US keyboard has.
 *
 * <p>⚠️ Every {@code SDLKeycode.SDLK_*} here is a compile-time {@code static final int}, so the
 * references below are inlined by javac and this class never loads LWJGL's SDL bindings. That is what
 * lets the keymap be unit-tested, and what keeps it loadable on a dedicated server. Do not replace
 * the table with reflection over the constants.
 */
public final class KeyNames {
    /** No key. SDL's {@code SDLK_UNKNOWN}. */
    public static final int UNKNOWN = SDLKeycode.SDLK_UNKNOWN;

    private static final Int2ObjectMap<String> NAMES = new Int2ObjectOpenHashMap<>();
    private static final Object2IntMap<String> CODES = new Object2IntOpenHashMap<>();
    private static final Int2ObjectMap<String> DISPLAY = new Int2ObjectOpenHashMap<>();

    private KeyNames() {}

    static {
        for (int key = SDLKeycode.SDLK_A; key <= SDLKeycode.SDLK_Z; key++) {
            var name = String.valueOf((char) key);
            put(key, name, name.toUpperCase(Locale.ROOT));
        }
        for (int key = SDLKeycode.SDLK_0; key <= SDLKeycode.SDLK_9; key++) {
            var name = String.valueOf((char) key);
            put(key, name, name);
        }
        // F1–F12 and F13–F24 are two separate runs in SDL's numbering.
        for (int i = 0; i < 12; i++) {
            put(SDLKeycode.SDLK_F1 + i, "f" + (i + 1), "F" + (i + 1));
            put(SDLKeycode.SDLK_F13 + i, "f" + (i + 13), "F" + (i + 13));
        }
        // SDL puts keypad 0 after keypad 9.
        put(SDLKeycode.SDLK_KP_0, "kp_0", "Num 0");
        for (int i = 1; i <= 9; i++) {
            put(SDLKeycode.SDLK_KP_1 + i - 1, "kp_" + i, "Num " + i);
        }
        put(SDLKeycode.SDLK_KP_PERIOD, "kp_decimal", "Num .");
        put(SDLKeycode.SDLK_KP_DIVIDE, "kp_divide", "Num /");
        put(SDLKeycode.SDLK_KP_MULTIPLY, "kp_multiply", "Num *");
        put(SDLKeycode.SDLK_KP_MINUS, "kp_subtract", "Num -");
        put(SDLKeycode.SDLK_KP_PLUS, "kp_add", "Num +");
        put(SDLKeycode.SDLK_KP_ENTER, "kp_enter", "Num Enter");
        put(SDLKeycode.SDLK_KP_EQUALS, "kp_equal", "Num =");

        put(SDLKeycode.SDLK_SPACE, "space", "Space");
        put(SDLKeycode.SDLK_APOSTROPHE, "apostrophe", "'");
        put(SDLKeycode.SDLK_COMMA, "comma", ",");
        put(SDLKeycode.SDLK_MINUS, "minus", "-");
        put(SDLKeycode.SDLK_PERIOD, "period", ".");
        put(SDLKeycode.SDLK_SLASH, "slash", "/");
        put(SDLKeycode.SDLK_SEMICOLON, "semicolon", ";");
        put(SDLKeycode.SDLK_EQUALS, "equal", "=");
        put(SDLKeycode.SDLK_LEFTBRACKET, "left_bracket", "[");
        put(SDLKeycode.SDLK_BACKSLASH, "backslash", "\\");
        put(SDLKeycode.SDLK_RIGHTBRACKET, "right_bracket", "]");
        put(SDLKeycode.SDLK_GRAVE, "grave_accent", "`");

        put(SDLKeycode.SDLK_ESCAPE, "escape", "Esc");
        put(SDLKeycode.SDLK_RETURN, "enter", "Enter");
        put(SDLKeycode.SDLK_TAB, "tab", "Tab");
        put(SDLKeycode.SDLK_BACKSPACE, "backspace", "Backspace");
        put(SDLKeycode.SDLK_INSERT, "insert", "Insert");
        put(SDLKeycode.SDLK_DELETE, "delete", "Delete");
        put(SDLKeycode.SDLK_RIGHT, "right", "Right");
        put(SDLKeycode.SDLK_LEFT, "left", "Left");
        put(SDLKeycode.SDLK_DOWN, "down", "Down");
        put(SDLKeycode.SDLK_UP, "up", "Up");
        put(SDLKeycode.SDLK_PAGEUP, "page_up", "Page Up");
        put(SDLKeycode.SDLK_PAGEDOWN, "page_down", "Page Down");
        put(SDLKeycode.SDLK_HOME, "home", "Home");
        put(SDLKeycode.SDLK_END, "end", "End");
        put(SDLKeycode.SDLK_CAPSLOCK, "caps_lock", "Caps Lock");
        put(SDLKeycode.SDLK_SCROLLLOCK, "scroll_lock", "Scroll Lock");
        put(SDLKeycode.SDLK_NUMLOCKCLEAR, "num_lock", "Num Lock");
        put(SDLKeycode.SDLK_PRINTSCREEN, "print_screen", "Print Screen");
        put(SDLKeycode.SDLK_PAUSE, "pause", "Pause");
        // The context-menu key on a PC keyboard; SDL's own SDLK_MENU is a rarer key of the same name.
        put(SDLKeycode.SDLK_APPLICATION, "menu", "Menu");
    }

    private static void put(int keyCode, String name, String display) {
        NAMES.put(keyCode, name);
        CODES.put(name, keyCode);
        DISPLAY.put(keyCode, display);
    }

    /**
     * Whether {@code keyCode} is a character a layout types rather than a named key: SDL keycodes
     * without the scancode bit are Unicode code points. Controls are excluded — Enter, Tab and friends
     * are in the table under their names.
     */
    private static boolean isCharacterKey(int keyCode) {
        return keyCode > SDLKeycode.SDLK_SPACE
                && (keyCode & SDLKeycode.SDLK_SCANCODE_MASK) == 0
                && Character.isValidCodePoint(keyCode)
                && !Character.isISOControl(keyCode)
                // SDL reports a letter key as its lower-case letter whatever the shift state, so an
                // upper-case code point is never a key, and naming one would read back as another.
                && Character.toLowerCase(keyCode) == keyCode;
    }

    /**
     * The stored name of a key, or {@code null} for one that cannot be named — an unbound chord, a
     * modifier key, or a non-printing key this table does not cover.
     */
    public static String nameOf(int keyCode) {
        var name = NAMES.get(keyCode);
        if (name != null) return name;
        return isCharacterKey(keyCode) ? Character.toString(keyCode) : null;
    }

    /** The key a stored name refers to, or {@link #UNKNOWN} if the name is not one of ours. */
    public static int codeOf(String name) {
        var lower = name.toLowerCase(Locale.ROOT);
        var code = CODES.getOrDefault(lower, UNKNOWN);
        if (code != UNKNOWN) return code;
        // A single character is a key named by what it types.
        if (lower.codePointCount(0, lower.length()) == 1 && isCharacterKey(lower.codePointAt(0))) {
            return lower.codePointAt(0);
        }
        return UNKNOWN;
    }

    /** How the key is written in the UI. Falls back to the raw code so an unknown key is still identifiable. */
    public static String displayOf(int keyCode) {
        var display = DISPLAY.get(keyCode);
        if (display != null) return display;
        if (isCharacterKey(keyCode)) return Character.toString(keyCode).toUpperCase(Locale.ROOT);
        return "Key " + keyCode;
    }

    /**
     * Whether the key is a modifier. A chord is the modifiers <em>plus</em> a key, so a modifier on its
     * own never forms one — pressing Shift while assigning a shortcut must not end the assignment.
     */
    public static boolean isModifier(int keyCode) {
        return switch (keyCode) {
            case SDLKeycode.SDLK_LSHIFT, SDLKeycode.SDLK_RSHIFT,
                 SDLKeycode.SDLK_LCTRL, SDLKeycode.SDLK_RCTRL,
                 SDLKeycode.SDLK_LALT, SDLKeycode.SDLK_RALT,
                 SDLKeycode.SDLK_LGUI, SDLKeycode.SDLK_RGUI -> true;
            default -> false;
        };
    }
}
