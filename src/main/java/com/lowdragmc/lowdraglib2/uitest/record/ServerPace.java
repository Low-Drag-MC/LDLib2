package com.lowdragmc.lowdraglib2.uitest.record;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.LockSupport;

/**
 * Holds a take back while the integrated server catches up with video time. A take can render several
 * times faster than real time; without this the server would fall behind and whatever it drives (synced
 * values, block entities) would lag in the video.
 *
 * <p>Gives up rather than stall: a paused, frozen or sprinting server, or one too far behind to catch up,
 * is followed from where it is instead.
 */
final class ServerPace {

    private static final long MAX_BEHIND = 10;
    private static final long TOLERANCE = 2;
    private static final long MAX_WAIT_NANOS = 100_000_000L;

    private long baseTicks = -1;
    private double baseSeconds;

    void await(@Nullable MinecraftServer server, boolean paused, double videoSeconds) {
        if (server == null) return;
        var rate = server.tickRateManager();
        if (baseTicks < 0 || paused || rate.isFrozen() || rate.isSprinting()) {
            rebase(server, videoSeconds);
            return;
        }
        long expected = baseTicks + (long) ((videoSeconds - baseSeconds) * rate.tickrate());
        if (expected - server.getTickCount() > MAX_BEHIND) {
            rebase(server, videoSeconds);
            return;
        }
        long deadline = System.nanoTime() + MAX_WAIT_NANOS;
        while (expected - server.getTickCount() > TOLERANCE && server.isRunning() && System.nanoTime() < deadline) {
            LockSupport.parkNanos(200_000L);
        }
    }

    private void rebase(MinecraftServer server, double videoSeconds) {
        baseTicks = server.getTickCount();
        baseSeconds = videoSeconds;
    }
}
