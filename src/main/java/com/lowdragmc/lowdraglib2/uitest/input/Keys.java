package com.lowdragmc.lowdraglib2.uitest.input;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Keyboard helpers for scenarios: GLFW key constants are re-exported so a scenario does not have to
 * import LWJGL, and printable characters are mapped to the {@code (key, scanCode, modifiers)} triple
 * that {@link net.minecraft.client.gui.screens.Screen#keyPressed} expects.
 */
public final class Keys {

    public static final int ENTER = GLFW.GLFW_KEY_ENTER;
    public static final int ESCAPE = GLFW.GLFW_KEY_ESCAPE;
    public static final int TAB = GLFW.GLFW_KEY_TAB;
    public static final int BACKSPACE = GLFW.GLFW_KEY_BACKSPACE;
    public static final int DELETE = GLFW.GLFW_KEY_DELETE;
    public static final int UP = GLFW.GLFW_KEY_UP;
    public static final int DOWN = GLFW.GLFW_KEY_DOWN;
    public static final int LEFT = GLFW.GLFW_KEY_LEFT;
    public static final int RIGHT = GLFW.GLFW_KEY_RIGHT;
    public static final int HOME = GLFW.GLFW_KEY_HOME;
    public static final int END = GLFW.GLFW_KEY_END;
    public static final int SPACE = GLFW.GLFW_KEY_SPACE;

    public static final int LEFT_SHIFT = GLFW.GLFW_KEY_LEFT_SHIFT;
    public static final int LEFT_CONTROL = GLFW.GLFW_KEY_LEFT_CONTROL;
    public static final int LEFT_ALT = GLFW.GLFW_KEY_LEFT_ALT;

    public static final int MOD_SHIFT = GLFW.GLFW_MOD_SHIFT;
    public static final int MOD_CONTROL = GLFW.GLFW_MOD_CONTROL;
    public static final int MOD_ALT = GLFW.GLFW_MOD_ALT;

    public static final int MOUSE_LEFT = GLFW.GLFW_MOUSE_BUTTON_LEFT;
    public static final int MOUSE_RIGHT = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    public static final int MOUSE_MIDDLE = GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

    private Keys() {
    }

    /**
     * The platform scan code for a key. Vanilla passes this straight through to
     * {@code InputConstants.getKey(keyCode, scanCode)}, which key bindings compare against — so
     * synthesising a plausible one keeps bound-key handling working.
     */
    public static int scanCodeOf(int keyCode) {
        return GLFW.glfwGetKeyScancode(keyCode);
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
        if ((modifiers & MOD_CONTROL) != 0) keys.add(GLFW.GLFW_KEY_LEFT_CONTROL);
        if ((modifiers & MOD_SHIFT) != 0) keys.add(GLFW.GLFW_KEY_LEFT_SHIFT);
        if ((modifiers & MOD_ALT) != 0) keys.add(GLFW.GLFW_KEY_LEFT_ALT);
        return keys;
    }

    /** A readable name for a key code, for step labels and report entries. */
    public static String nameOf(int keyCode) {
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getName();
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
