package com.lowdragmc.lowdraglib2.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

public final class UISoundPlayerClient {
    private static boolean installed;

    private UISoundPlayerClient() {
    }

    public static void installSharedHooks() {
        if (installed) {
            return;
        }
        UISoundUtils.setSoundPlayer(UISoundPlayerClient::play);
        installed = true;
    }

    private static void play(SoundEvent soundEvent, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch, volume));
    }
}
