package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import java.lang.reflect.Method;

final class UIElementInputAccess {
    private static final String MINECRAFT_CLASS = "net.minecraft.client.Minecraft";
    private static final String WINDOW_METHOD = "getWindow";

    private UIElementInputAccess() {
    }

    static boolean isShiftDown() {
        return invokeMinecraftBoolean("hasShiftDown");
    }

    static boolean isCtrlDown() {
        return invokeMinecraftBoolean("hasControlDown");
    }

    static boolean isAltDown() {
        return invokeMinecraftBoolean("hasAltDown");
    }

    static boolean isKeyDown(int keyCode) {
        if (!LDLib2.isClient()) {
            return false;
        }
        try {
            var minecraft = getMinecraftInstance();
            if (minecraft == null) {
                return false;
            }
            Method getWindow = minecraft.getClass().getMethod(WINDOW_METHOD);
            Object window = getWindow.invoke(minecraft);
            if (!(window instanceof Window clientWindow)) {
                return false;
            }
            return InputConstants.isKeyDown(clientWindow, keyCode);
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private static boolean invokeMinecraftBoolean(String methodName) {
        if (!LDLib2.isClient()) {
            return false;
        }
        try {
            Object minecraft = getMinecraftInstance();
            if (minecraft == null) {
                return false;
            }
            Method method = minecraft.getClass().getMethod(methodName);
            Object result = method.invoke(minecraft);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Object getMinecraftInstance() throws ReflectiveOperationException {
        Class<?> minecraftClass = Class.forName(MINECRAFT_CLASS);
        Method getInstance = minecraftClass.getMethod("getInstance");
        return getInstance.invoke(null);
    }
}
