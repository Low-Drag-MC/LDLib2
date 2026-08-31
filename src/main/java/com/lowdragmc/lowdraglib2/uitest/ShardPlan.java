package com.lowdragmc.lowdraglib2.uitest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Splits a scenario list across parallel test processes.</b>
 *
 * <p>Every shard runs this over the <i>same</i> full list and keeps only its own bucket, so no
 * process has to be told what to run and none can be told something another process disagrees with.
 * That works only because the answer is a pure function of {@code (names, weights, jobs)} — no
 * clock, no randomness, no iteration order of a hash map — and because
 * {@code UITestRunner#collectScenarios} hands it a name-sorted list.
 *
 * <p>Balancing uses the durations of a previous run, which the orchestrator carries forward. Greedy
 * longest-processing-time-first, which for this shape is within a few percent of optimal and, unlike
 * round-robin, cannot pile the several slowest scenarios onto one shard. Measured against a real
 * 20-scenario report (1.4s–9.6s each): at 4 jobs, 25.7s → 22.7s; at 6, 20.5s → 15.6s.
 *
 * <p>With no weights it degrades to round-robin over the sorted names, which is what the very first
 * parallel run does. It gets better by itself from the second run on.
 *
 * <p>Deliberately free of Minecraft and of every other LDLib2 class, so it is unit-testable —
 * see {@code ShardPlanTest}. The properties that matter are not "it is fast" but <b>every name is
 * assigned exactly once</b> and <b>two processes agree</b>; a bug in either silently drops a
 * scenario from the run, and a dropped scenario is a green build that tested nothing.
 */
public final class ShardPlan {

    /** What an unseen scenario is assumed to cost: the median of what is known, so it is neither. */
    private static final long UNKNOWN_WEIGHT_FALLBACK = 3_000L;

    private ShardPlan() {
    }

    /**
     * Assigns each name a shard index in {@code [0, jobs)}.
     *
     * @param sortedNames every scenario in the selection, in a stable order. Not re-sorted here: the
     *                    caller's order is part of the contract, because it is what makes two
     *                    processes agree.
     * @param weightsMs   name to previously measured duration; may be empty or partial
     * @param jobs        number of shards; anything below 2 puts everything on shard 0
     * @return name to shard index, in the input's order
     */
    public static Map<String, Integer> assign(List<String> sortedNames, Map<String, Long> weightsMs, int jobs) {
        var assignment = new LinkedHashMap<String, Integer>();
        if (sortedNames.isEmpty()) return assignment;
        if (jobs < 2) {
            sortedNames.forEach(name -> assignment.put(name, 0));
            return assignment;
        }
        if (weightsMs == null || weightsMs.isEmpty()) {
            for (int i = 0; i < sortedNames.size(); i++) {
                assignment.put(sortedNames.get(i), i % jobs);
            }
            return assignment;
        }

        var fallback = medianOf(weightsMs);
        // Heaviest first, ties broken by name: the tie-break is what makes this deterministic, and
        // without it two processes with differently-ordered equal weights would diverge.
        var order = new ArrayList<>(sortedNames);
        order.sort(Comparator.comparingLong((String name) -> -weightOf(name, weightsMs, fallback))
                .thenComparing(Comparator.naturalOrder()));

        var load = new long[jobs];
        var perShard = new LinkedHashMap<String, Integer>();
        for (var name : order) {
            int lightest = 0;
            for (int shard = 1; shard < jobs; shard++) {
                if (load[shard] < load[lightest]) lightest = shard;
            }
            load[lightest] += weightOf(name, weightsMs, fallback);
            perShard.put(name, lightest);
        }
        // Rebuilt in the caller's order so the result reads like the input, not like the LPT order.
        for (var name : sortedNames) {
            assignment.put(name, perShard.get(name));
        }
        return assignment;
    }

    /** The names {@code shardIndex} is responsible for, in the input's order. */
    public static List<String> shardOf(List<String> sortedNames, Map<String, Long> weightsMs,
                                       int jobs, int shardIndex) {
        var assignment = assign(sortedNames, weightsMs, jobs);
        return sortedNames.stream().filter(name -> assignment.get(name) == shardIndex).toList();
    }

    private static long weightOf(String name, Map<String, Long> weightsMs, long fallback) {
        var weight = weightsMs.get(name);
        // A scenario added since the last run has no weight, and must still be placed somewhere -
        // dropping it would be the one failure this class exists to prevent.
        return weight == null || weight <= 0 ? fallback : weight;
    }

    private static long medianOf(Map<String, Long> weightsMs) {
        var values = weightsMs.values().stream().filter(value -> value > 0).sorted().toList();
        if (values.isEmpty()) return UNKNOWN_WEIGHT_FALLBACK;
        return values.get(values.size() / 2);
    }
}
