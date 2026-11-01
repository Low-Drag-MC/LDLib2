package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;

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
        // the typed rules rather than commands: a renamed rule fails to compile instead of failing silently
        var rules = server.getGameRules();
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        rules.set(GameRules.SPAWN_MOBS, false, server);
        rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, server);
        rules.set(GameRules.RANDOM_TICK_SPEED, 0, server);
        rules.set(GameRules.SPAWN_WANDERING_TRADERS, false, server);
        rules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server);
        var commands = server.getCommands();
        // Suppressed, or every one of these broadcasts into chat and the harness photographs its own
        // setup in the first screenshot of every run.
        var source = server.createCommandSourceStack().withSuppressedOutput();
        for (var command : new String[]{
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
