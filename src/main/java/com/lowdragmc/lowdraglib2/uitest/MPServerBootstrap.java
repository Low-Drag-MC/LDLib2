package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.uitest.mp.MPRunConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Arms {@link MPServerRunner} when this process is the dedicated server of a multi-process run —
 * that is, when the {@code runMpServer} Gradle run set the mptest role/hub system properties.
 * Inert everywhere else, including ordinary {@code runServer} launches.
 */
@EventBusSubscriber(modid = LDLib2.MOD_ID)
public final class MPServerBootstrap {

    @Nullable
    private static MPServerRunner runner;

    private MPServerBootstrap() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Platform.isDevEnv()) return;
        var config = MPRunConfig.fromSystemProperties();
        if (config == null || !config.isServer()) return;
        runner = new MPServerRunner(config, event.getServer());
        runner.start();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (runner != null) {
            runner.tick();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        if (runner != null) {
            runner.close();
            runner = null;
        }
    }
}
