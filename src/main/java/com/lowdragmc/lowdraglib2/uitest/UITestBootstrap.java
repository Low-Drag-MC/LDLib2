package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * The frame pump, plus a watchdog so a hung game still produces a report.
 */
@EventBusSubscriber(modid = LDLib2.MOD_ID, value = Dist.CLIENT)
public final class UITestBootstrap {

    private static boolean bootstrapped;
    private static Thread watchdog;

    private UITestBootstrap() {
    }

    /**
     * Must be a whole-frame hook, not {@code ScreenEvent.Render.Post}: that one only fires while a
     * screen is open, so the harness would stall the moment the world finishes loading and nothing
     * is on screen — and only resume if a human pressed escape. This still runs post-render, so
     * captures taken from it are valid.
     */
    @SubscribeEvent
    public static void onFrameRendered(RenderFrameEvent.Post event) {
        // The scenario registry only exists in a dev environment, so outside one this hook can never
        // have anything to do — do not even class-load the runner on a production client.
        if (!Platform.isDevEnv()) return;
        if (!bootstrapped) {
            bootstrapped = true;
            UITestRunner.bootstrapIfRequested();
            startWatchdog();
        }
        var runner = UITestRunner.active();
        if (runner != null) {
            runner.onFrame();
        }
    }

    /**
     * Kills a run that stops producing frames — a deadlock, a modal dialog nothing will dismiss, or
     * chunk generation that never finishes. Without this a CI job hangs until its own timeout and
     * produces nothing at all; with it, there is a report naming the last step and a full thread dump.
     */
    private static void startWatchdog() {
        var runner = UITestRunner.active();
        if (runner == null || watchdog != null) return;
        long timeoutNanos = runner.config().watchdogSeconds() * 1_000_000_000L;
        watchdog = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                var active = UITestRunner.active();
                if (active == null) return;
                // Shutdown legitimately stops producing frames. Without this the watchdog fires at
                // the end of every successful run and overwrites a good report with a hang report.
                if (active.isShuttingDown()) return;
                if (System.nanoTime() - active.lastHeartbeatNanos() > timeoutNanos) {
                    var dump = dumpThreads();
                    LDLib2.LOGGER.error("[uitest] no rendered frame for {}s - the game appears hung.\n{}",
                            runner.config().watchdogSeconds(), dump);
                    try {
                        active.reportHang(dump);
                    } catch (Throwable t) {
                        LDLib2.LOGGER.error("[uitest] could not write the hang report", t);
                    }
                    // halt, not exit: shutdown hooks would join threads that are exactly what is stuck.
                    Runtime.getRuntime().halt(3);
                }
            }
        }, "ldlib2-uitest-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static String dumpThreads() {
        var out = new StringBuilder();
        for (var entry : Thread.getAllStackTraces().entrySet()) {
            out.append("\"").append(entry.getKey().getName()).append("\" ")
                    .append(entry.getKey().getState()).append('\n');
            for (var frame : entry.getValue()) {
                out.append("    at ").append(frame).append('\n');
            }
        }
        return out.toString();
    }
}
