package com.lowdragmc.lowdraglib2.editor.keymap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.glfw.GLFW;

/**
 * The stable text names key codes are stored under, and the pretty ones they are shown under.
 *
 * <p>Stored names are this library's own rather than GLFW numbers or Minecraft's {@code key.keyboard.*}
 * translation keys: a number in a config file means nothing to a human editing it, and it would also
 * tie a saved keymap to the exact GLFW build the numbers came from.
 *
 * <p>⚠️ Every {@code GLFW.GLFW_KEY_*} here is a compile-time {@code static final int}, so the
 * references below are inlined by javac and this class never loads {@code org.lwjgl.glfw.GLFW} — which
 * would try to load a native library. That is what lets the keymap be unit-tested, and what keeps it
 * loadable on a dedicated server. Do not replace the table with reflection over GLFW's fields.
 */
public final class KeyNames {
    private static final Int2ObjectMap<String> NAMES = new Int2ObjectOpenHashMap<>();
    private static final Object2IntMap<String> CODES = new Object2IntOpenHashMap<>();
    private static final Int2ObjectMap<String> DISPLAY = new Int2ObjectOpenHashMap<>();

    private KeyNames() {}

    static {
        for (int key = GLFW.GLFW_KEY_A; key <= GLFW.GLFW_KEY_Z; key++) {
            var name = String.valueOf((char) ('a' + key - GLFW.GLFW_KEY_A));
            put(key, name, name.toUpperCase());
        }
        for (int key = GLFW.GLFW_KEY_0; key <= GLFW.GLFW_KEY_9; key++) {
            var name = String.valueOf((char) ('0' + key - GLFW.GLFW_KEY_0));
            put(key, name, name);
        }
        for (int key = GLFW.GLFW_KEY_F1; key <= GLFW.GLFW_KEY_F25; key++) {
            var name = "f" + (key - GLFW.GLFW_KEY_F1 + 1);
            put(key, name, name.toUpperCase());
        }
        for (int key = GLFW.GLFW_KEY_KP_0; key <= GLFW.GLFW_KEY_KP_9; key++) {
            var digit = String.valueOf(key - GLFW.GLFW_KEY_KP_0);
            put(key, "kp_" + digit, "Num " + digit);
        }
        put(GLFW.GLFW_KEY_KP_DECIMAL, "kp_decimal", "Num .");
        put(GLFW.GLFW_KEY_KP_DIVIDE, "kp_divide", "Num /");
        put(GLFW.GLFW_KEY_KP_MULTIPLY, "kp_multiply", "Num *");
        put(GLFW.GLFW_KEY_KP_SUBTRACT, "kp_subtract", "Num -");
        put(GLFW.GLFW_KEY_KP_ADD, "kp_add", "Num +");
        put(GLFW.GLFW_KEY_KP_ENTER, "kp_enter", "Num Enter");
        put(GLFW.GLFW_KEY_KP_EQUAL, "kp_equal", "Num =");

        put(GLFW.GLFW_KEY_SPACE, "space", "Space");
        put(GLFW.GLFW_KEY_APOSTROPHE, "apostrophe", "'");
        put(GLFW.GLFW_KEY_COMMA, "comma", ",");
        put(GLFW.GLFW_KEY_MINUS, "minus", "-");
        put(GLFW.GLFW_KEY_PERIOD, "period", ".");
        put(GLFW.GLFW_KEY_SLASH, "slash", "/");
        put(GLFW.GLFW_KEY_SEMICOLON, "semicolon", ";");
        put(GLFW.GLFW_KEY_EQUAL, "equal", "=");
        put(GLFW.GLFW_KEY_LEFT_BRACKET, "left_bracket", "[");
        put(GLFW.GLFW_KEY_BACKSLASH, "backslash", "\\");
        put(GLFW.GLFW_KEY_RIGHT_BRACKET, "right_bracket", "]");
        put(GLFW.GLFW_KEY_GRAVE_ACCENT, "grave_accent", "`");

        put(GLFW.GLFW_KEY_ESCAPE, "escape", "Esc");
        put(GLFW.GLFW_KEY_ENTER, "enter", "Enter");
        put(GLFW.GLFW_KEY_TAB, "tab", "Tab");
        put(GLFW.GLFW_KEY_BACKSPACE, "backspace", "Backspace");
        put(GLFW.GLFW_KEY_INSERT, "insert", "Insert");
        put(GLFW.GLFW_KEY_DELETE, "delete", "Delete");
        put(GLFW.GLFW_KEY_RIGHT, "right", "Right");
        put(GLFW.GLFW_KEY_LEFT, "left", "Left");
        put(GLFW.GLFW_KEY_DOWN, "down", "Down");
        put(GLFW.GLFW_KEY_UP, "up", "Up");
        put(GLFW.GLFW_KEY_PAGE_UP, "page_up", "Page Up");
        put(GLFW.GLFW_KEY_PAGE_DOWN, "page_down", "Page Down");
        put(GLFW.GLFW_KEY_HOME, "home", "Home");
        put(GLFW.GLFW_KEY_END, "end", "End");
        put(GLFW.GLFW_KEY_CAPS_LOCK, "caps_lock", "Caps Lock");
        put(GLFW.GLFW_KEY_SCROLL_LOCK, "scroll_lock", "Scroll Lock");
        put(GLFW.GLFW_KEY_NUM_LOCK, "num_lock", "Num Lock");
        put(GLFW.GLFW_KEY_PRINT_SCREEN, "print_screen", "Print Screen");
        put(GLFW.GLFW_KEY_PAUSE, "pause", "Pause");
        put(GLFW.GLFW_KEY_MENU, "menu", "Menu");
    }

    private static void put(int keyCode, String name, String display) {
        NAMES.put(keyCode, name);
        CODES.put(name, keyCode);
        DISPLAY.put(keyCode, display);
    }

    /**
     * The stored name of a key, or {@code null} for one this table does not cover — an unbound chord,
     * a modifier key, or something exotic that no keyboard here has.
     */
    public static String nameOf(int keyCode) {
        return NAMES.get(keyCode);
    }

    /** The key a stored name refers to, or {@link GLFW#GLFW_KEY_UNKNOWN} if the name is not one of ours. */
    public static int codeOf(String name) {
        return CODES.getOrDefault(name.toLowerCase(), GLFW.GLFW_KEY_UNKNOWN);
    }

    /** How the key is written in the UI. Falls back to the raw code so an unknown key is still identifiable. */
    public static String displayOf(int keyCode) {
        var display = DISPLAY.get(keyCode);
        return display == null ? "Key " + keyCode : display;
    }

    /**
     * Whether the key is a modifier. A chord is the modifiers <em>plus</em> a key, so a modifier on its
     * own never forms one — pressing Shift while assigning a shortcut must not end the assignment.
     */
    public static boolean isModifier(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
                 GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
                 GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
                 GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> true;
            default -> false;
        };
    }
}
