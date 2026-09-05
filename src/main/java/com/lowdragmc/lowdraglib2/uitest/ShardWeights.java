package com.lowdragmc.lowdraglib2.uitest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * How long each scenario took last time, carried between runs so {@link ShardPlan} can even out the
 * slices of a parallel run.
 *
 * <p>Read inside the game by each shard and written outside it by the orchestrator, which is the
 * only reason this is its own class: a file format with a reader in one process and a writer in
 * another is exactly the kind of thing that drifts when each side spells it out for itself.
 *
 * <p>Nothing here may fail a run. An unreadable or absent file means an unbalanced split, and an
 * unbalanced split is slow, never wrong — so every path returns empty rather than throwing.
 */
public final class ShardWeights {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final java.lang.reflect.Type TYPE = new TypeToken<TreeMap<String, Long>>() {}.getType();

    private ShardWeights() {
    }

    /** Scenario name to duration in milliseconds; empty when there is nothing recorded yet. */
    public static Map<String, Long> read(@org.jetbrains.annotations.Nullable Path file) {
        if (file == null || !Files.isRegularFile(file)) return Map.of();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Long> weights = GSON.fromJson(reader, TYPE);
            return weights == null ? Map.of() : weights;
        } catch (IOException | RuntimeException e) {
            return Map.of();
        }
    }

    /**
     * Merges {@code durations} over whatever is already recorded and writes the result.
     *
     * <p>Merged rather than replaced so that running a subset — {@code -PldTest=tag:editor} — does
     * not throw away what is known about everything else, which would un-balance the next full run.
     *
     * @return the number of scenarios now on record, or {@code -1} if it could not be written
     */
    public static int merge(Path file, Map<String, Long> durations) {
        var weights = new TreeMap<>(read(file));
        durations.forEach((name, duration) -> {
            if (duration > 0) weights.put(name, duration);
        });
        if (weights.isEmpty()) return 0;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(weights), StandardCharsets.UTF_8);
            return weights.size();
        } catch (IOException e) {
            return -1;
        }
    }
}
