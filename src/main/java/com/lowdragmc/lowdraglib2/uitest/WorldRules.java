package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.server.MinecraftServer;

/**
 * The determinism rules every test world gets, factored out of the client-only
 * {@link WorldBootstrap} so the dedicated server of a multi-process run can apply them too.
 */
public final class WorldRules {

    private WorldRules() {
    }

    /**
     * Freezes everything that would otherwise make two runs differ: time of day, weather, mob
     * spawning and random ticks. Without this, screenshots taken a few seconds apart can differ in
     * lighting alone, and a wandering mob can walk into frame.
     */
    public static void pin(MinecraftServer server) {
        var commands = server.getCommands();
        // Suppressed, or every one of these broadcasts into chat and the harness photographs its own
        // setup in the first screenshot of every run.
        var source = server.createCommandSourceStack().withSuppressedOutput();
        for (var command : new String[]{
                "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false",
                "gamerule doMobSpawning false",
                "gamerule doFireTick false",
                "gamerule randomTickSpeed 0",
                "gamerule doTraderSpawning false",
                "gamerule announceAdvancements false",
                "time set noon",
                "weather clear",
                "difficulty peaceful",
                "kill @e[type=!player]",
        }) {
            try {
                commands.performPrefixedCommand(source, command);
            } catch (Exception e) {
                LDLib2.LOGGER.warn("[uitest] could not apply '{}'", command, e);
            }
        }
    }
}
