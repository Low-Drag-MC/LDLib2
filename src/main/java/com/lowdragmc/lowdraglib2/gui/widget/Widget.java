package com.lowdragmc.lowdraglib2.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class Widget {
    @OnlyIn(Dist.CLIENT)
    public static void playButtonClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isShiftDown() {
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isAltDown() {
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isKeyDown(int keyCode) {
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, keyCode);
    }

}
