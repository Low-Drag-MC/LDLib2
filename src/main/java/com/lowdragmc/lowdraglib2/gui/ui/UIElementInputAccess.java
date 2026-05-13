package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputQuirks;
import org.lwjgl.glfw.GLFW;

final class UIElementInputAccess {

    private UIElementInputAccess() {
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

    static boolean isCtrlOrCmdDown() {
        if (!LDLib2.isClient()) {
            return false;
        }
        if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            var minecraft = Minecraft.getInstance();
            var window = minecraft.getWindow();
            return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER);
        }
        return isCtrlDown();
    }

    static boolean isAltDown() {
        if (!LDLib2.isClient()) {
            return false;
        }
        return Minecraft.getInstance().hasAltDown();
    }

    static boolean isKeyDown(int keyCode) {
        if (!LDLib2.isClient()) {
            return false;
        }
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, keyCode);
    }
}
