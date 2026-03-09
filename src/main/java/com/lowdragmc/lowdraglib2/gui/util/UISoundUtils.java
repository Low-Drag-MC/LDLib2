package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.experimental.UtilityClass;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

@UtilityClass
public class UISoundUtils {
    private static SoundPlayer soundPlayer = (soundEvent, pitch, volume) -> { };

    public static void playButtonClickSound() {
        playSound(SoundEvents.UI_BUTTON_CLICK);
    }

    public static void playSound(Holder<SoundEvent> soundHolder) {
        playSound(soundHolder.value(), 1f, 0.25F);
    }

    public static void playSound(SoundEvent soundEvent) {
        playSound(soundEvent, 1f, 0.25F);
    }

    public static void playSound(SoundEvent soundEvent, float pitch) {
        playSound(soundEvent, pitch, 0.25F);
    }

    public static void playSound(SoundEvent soundEvent, float pitch, float volume) {
        if (LDLib2.isClient()) {
            soundPlayer.play(soundEvent, pitch, volume);
        }
    }

    public static void setSoundPlayer(SoundPlayer soundPlayer) {
        UISoundUtils.soundPlayer = soundPlayer == null ? (soundEvent, pitch, volume) -> { } : soundPlayer;
    }

    @FunctionalInterface
    public interface SoundPlayer {
        void play(SoundEvent soundEvent, float pitch, float volume);
    }
}
