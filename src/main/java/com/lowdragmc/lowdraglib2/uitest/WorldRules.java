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
        // Set directly rather than by /gamerule: 26.1 renamed every rule (doDaylightCycle is advance_time), and a
        // command naming one that no longer exists fails into the suppressed source below without a word - the
        // clock kept running, and a long run reached night.
        var rules = server.getGameRules();
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        rules.set(GameRules.SPAWN_MOBS, false, server);
        // what doFireTick false was: fire spreads only this close to a player
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
