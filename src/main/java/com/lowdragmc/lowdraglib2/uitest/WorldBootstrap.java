package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Gets the client from "just launched" into a loaded, deterministic world without a human touching
 * the menus.
 */
@OnlyIn(Dist.CLIENT)
public final class WorldBootstrap {

    /** Prefix for worlds this harness owns, so leftovers can be swept without touching real saves. */
    public static final String WORLD_PREFIX = "ldtest_";

    private WorldBootstrap() {
    }

    public static String worldNameFor(String runId) {
        return WORLD_PREFIX + runId;
    }

    /**
     * Removes worlds left behind by earlier runs.
     *
     * <p>The single biggest source of flakiness. A run killed part-way through leaves a half-written
     * save or a held session lock, and every later run then fails with "Failed to access world" — a
     * failure that looks like a bug in whatever is being tested rather than leftover state. Each run
     * also gets a unique world name so two runs cannot collide even if a sweep misses something.
     */
    public static void deleteStaleWorlds(Minecraft minecraft) {
        var saves = new File(minecraft.gameDirectory, "saves").toPath();
        if (!Files.isDirectory(saves)) return;
        try (Stream<Path> entries = Files.list(saves)) {
            entries.filter(path -> path.getFileName().toString().startsWith(WORLD_PREFIX))
                    .forEach(WorldBootstrap::deleteRecursively);
        } catch (IOException e) {
            LDLib2.LOGGER.warn("[uitest] could not scan the saves directory for stale test worlds", e);
        }
    }

    private static void deleteRecursively(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    LDLib2.LOGGER.warn("[uitest] could not delete {}", path, e);
                }
            });
        } catch (IOException e) {
            LDLib2.LOGGER.warn("[uitest] could not delete stale world {}", root, e);
        }
    }

    /**
     * Creates and loads a fresh creative world, bypassing the world-selection screens entirely.
     *
     * <p>Superflat, deliberately. It generates in a fraction of the time of a normal world, and it
     * gives every screenshot the same flat backdrop instead of whatever terrain the seed happened to
     * produce — no trees, no wandering pigs, nothing that differs between runs behind the UI under
     * test.
     */
    public static void createFreshLevel(Minecraft minecraft, String worldName) {
        var settings = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(worldName, settings,
                new WorldOptions(0L, false, false),
                registryAccess -> registryAccess.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                null);
    }

    /**
     * Freezes everything that would otherwise make two runs differ. Delegates to {@link WorldRules}
     * so the multi-process dedicated server applies the identical set.
     */
    public static void pinWorldRules(net.minecraft.server.MinecraftServer server) {
        WorldRules.pin(server);
    }
}
