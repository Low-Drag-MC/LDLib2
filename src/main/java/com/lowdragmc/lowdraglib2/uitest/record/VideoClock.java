package com.lowdragmc.lowdraglib2.uitest.record;

import net.minecraft.util.Util;

/**
 * The clock a recording runs on: while a take is open, every rendered frame advances time by exactly
 * one video frame, however long the frame really took to render, read back and encode.
 *
 * <p>Installed with {@link Util#setTimeSource}, so it drives everything that reads {@link Util#getNanos()}
 * or {@link Util#getMillis()}: client ticks and partial ticks, the integrated server's tick loop, and
 * LDLib2's own animations, transitions, caret blink and double-click window, and whatever holds
 * {@link Util#timeSource()}, which reads through to the installed source. Anything that reads
 * {@code System.nanoTime()} directly is outside it and runs at wall-clock speed in the video.
 *
 * <p>Once installed it stays installed for the life of the process. Between takes it reports wall-clock
 * time plus a fixed offset, chosen when the last take closed, so time never jumps or runs backwards when
 * a take opens or closes. That is also why a take is free to render faster than real time.
 *
 * <p>Render thread only, except {@link #now()}, which the server thread reads through
 * {@link Util#getNanos()}.
 */
public final class VideoClock {

    private static boolean installed;
    private static volatile boolean stepping;
    private static volatile long offsetNanos;
    private static volatile long frameNanos;
    private static long baseNanos;
    private static long frames;
    private static int fps;

    private VideoClock() {
    }

    private static long now() {
        return stepping ? frameNanos : System.nanoTime() + offsetNanos;
    }

    /** Whether a take currently owns time. */
    public static boolean isStepping() {
        return stepping;
    }

    /** Frames advanced since the current take began; {@code 0} outside a take. */
    public static long frames() {
        return stepping ? frames : 0;
    }

    static void beginStepping(int framesPerSecond) {
        if (stepping) throw new IllegalStateException("a take is already stepping the clock");
        if (!installed) {
            // the game's own source (the graphics backend's timer) is replaced by this one, so read it first
            offsetNanos = Util.getNanos() - System.nanoTime();
            Util.setTimeSource(VideoClock::now);
            installed = true;
        }
        fps = framesPerSecond;
        frames = 0;
        baseNanos = now();
        frameNanos = baseNanos;
        stepping = true;
    }

    static void advanceFrame() {
        if (!stepping) return;
        frames++;
        frameNanos = baseNanos + frames * 1_000_000_000L / fps;
    }

    static void endStepping() {
        if (!stepping) return;
        offsetNanos = frameNanos - System.nanoTime();
        stepping = false;
    }
}
