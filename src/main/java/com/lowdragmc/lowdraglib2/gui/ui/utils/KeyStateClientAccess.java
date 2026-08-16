package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputQuirks;
import org.lwjgl.glfw.GLFW;

/**
 * The client half of {@link KeyState}: the real keyboard, read from the game window.
 *
 * <p>Split out so {@link KeyState} itself carries no client types. Every entry point checks
 * {@link LDLib2#isClient()} before touching {@link Minecraft}, so a stray call on a dedicated server
 * answers "not held" instead of failing to link.
 */
final class KeyStateClientAccess {

    private KeyStateClientAccess() {
    }

    static boolean isKeyDown(int keyCode) {
        if (!LDLib2.isClient()) {
            return false;
        }
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), keyCode);
    }

    static boolean isShiftDown() {
        if (!LDLib2.isClient()) {
            return false;
        }
        return Minecraft.getInstance().hasShiftDown();
    }

    static boolean isCtrlDown() {
        if (!LDLib2.isClient()) {
            return false;
        }
        return Minecraft.getInstance().hasControlDown();
    }

    static boolean isAltDown() {
        if (!LDLib2.isClient()) {
            return false;
        }
        return Minecraft.getInstance().hasAltDown();
    }

    static boolean isCtrlOrCmdDown() {
        if (!LDLib2.isClient()) {
            return false;
        }
        if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            var window = Minecraft.getInstance().getWindow();
            return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER);
        }
        return isCtrlDown();
    }
}
