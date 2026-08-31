package com.lowdragmc.lowdraglib2.uitest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ShardPlan} decides, independently inside every parallel test process, which scenarios that
 * process is responsible for. Nothing reconciles the answers afterwards, so the properties worth
 * testing are not about speed:
 *
 * <ul>
 *   <li><b>completeness</b> — a name assigned to no shard runs nowhere, and a run that tested
 *       nothing still reports green;</li>
 *   <li><b>disjointness</b> — a name assigned twice wastes a slot and can double-report;</li>
 *   <li><b>determinism</b> — two processes computing different plans break both of the above, and
 *       would do so intermittently.</li>
 * </ul>
 *
 * Balance is tested too, but it is the one property whose failure only costs time.
 */
class ShardPlanTest {

    /** The real durations from a 20-scenario run, which is what the balancing was tuned against. */
    private static final Map<String, Long> REAL_WEIGHTS = new LinkedHashMap<>();

    static {
        REAL_WEIGHTS.put("ngt_wire_reroute", 9646L);
        REAL_WEIGHTS.put("wiki_component_gallery", 9497L);
        REAL_WEIGHTS.put("floating_view", 6523L);
        REAL_WEIGHTS.put("ui_debugger", 6430L);
        REAL_WEIGHTS.put("graph_lod", 6086L);
        REAL_WEIGHTS.put("ui_debugger_window", 5940L);
        REAL_WEIGHTS.put("editor_save_notification", 5662L);
        REAL_WEIGHTS.put("ui_debugger_multi_window", 5222L);
        REAL_WEIGHTS.put("wiki_captures", 5135L);
        REAL_WEIGHTS.put("editor_pane_maximize", 3888L);
        REAL_WEIGHTS.put("floating_scene", 3750L);
        REAL_WEIGHTS.put("wiki_component_special", 3023L);
        REAL_WEIGHTS.put("gizmo_rotate", 2991L);
        REAL_WEIGHTS.put("resource_view_pinned_tab", 2874L);
        REAL_WEIGHTS.put("sync_hooks", 2674L);
        REAL_WEIGHTS.put("snake_hud", 2604L);
        REAL_WEIGHTS.put("scroller_shift_axis", 2496L);
        REAL_WEIGHTS.put("ngt_node_description", 1567L);
        REAL_WEIGHTS.put("asset_browser_literal_names", 1490L);
        REAL_WEIGHTS.put("window_tooltip_bounds", 1440L);
    }

    private static List<String> realNames() {
        return REAL_WEIGHTS.keySet().stream().sorted().toList();
    }

    private static List<String> names(int count) {
        return IntStream.range(0, count).mapToObj("scenario_%02d"::formatted).toList();
    }

    // ---- completeness and disjointness ----

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 6, 8, 20, 32})
    @DisplayName("every scenario lands on exactly one shard, weighted or not")
    void everyScenarioIsAssignedExactlyOnce(int jobs) {
        for (var weights : List.of(Map.<String, Long>of(), REAL_WEIGHTS)) {
            var sorted = realNames();
            var seen = new ArrayList<String>();
            for (int shard = 0; shard < jobs; shard++) {
                seen.addAll(ShardPlan.shardOf(sorted, weights, jobs, shard));
            }
            assertEquals(sorted.size(), seen.size(),
                    "jobs=" + jobs + " weighted=" + !weights.isEmpty() + " assigned a name twice or not at all");
            assertEquals(sorted.stream().sorted().toList(), seen.stream().sorted().toList(),
                    "jobs=" + jobs + " weighted=" + !weights.isEmpty() + " lost or invented a name");
        }
    }

    @Test
    @DisplayName("a scenario with no recorded weight is still placed")
    void unknownScenariosAreStillAssigned() {
        var sorted = new ArrayList<>(realNames());
        sorted.add("brand_new_scenario");
        sorted.sort(null);

        var assignment = ShardPlan.assign(sorted, REAL_WEIGHTS, 4);
        assertEquals(sorted.size(), assignment.size(), "the new scenario was dropped");
        assertTrue(assignment.containsKey("brand_new_scenario"));
    }

    @Test
    @DisplayName("shard indices stay inside [0, jobs)")
    void shardIndicesAreInRange() {
        for (var shard : ShardPlan.assign(realNames(), REAL_WEIGHTS, 5).values()) {
            assertTrue(shard >= 0 && shard < 5, "out-of-range shard " + shard);
        }
    }

    // ---- determinism: the property two processes depend on ----

    @Test
    @DisplayName("the same inputs give the same plan, whatever order the weights map iterates in")
    void planIsDeterministicAcrossMapOrdering() {
        var sorted = realNames();
        var shuffled = new HashMap<String, Long>();
        // A HashMap iterates in hash order, not insertion order - if the plan depended on iteration
        // order at all, this is where two processes would quietly disagree.
        REAL_WEIGHTS.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .forEach(entry -> shuffled.put(entry.getKey(), entry.getValue()));

        assertEquals(ShardPlan.assign(sorted, REAL_WEIGHTS, 4), ShardPlan.assign(sorted, shuffled, 4));
    }

    @Test
    @DisplayName("equal weights are broken by name rather than by chance")
    void tiesAreBrokenDeterministically() {
        var sorted = names(12);
        var flat = new LinkedHashMap<String, Long>();
        sorted.forEach(name -> flat.put(name, 1000L));
        var reversed = new LinkedHashMap<String, Long>();
        sorted.reversed().forEach(name -> reversed.put(name, 1000L));

        assertEquals(ShardPlan.assign(sorted, flat, 3), ShardPlan.assign(sorted, reversed, 3));
    }

    @Test
    @DisplayName("repeated calls agree with themselves")
    void planIsStableAcrossCalls() {
        var sorted = realNames();
        var first = ShardPlan.assign(sorted, REAL_WEIGHTS, 6);
        for (int i = 0; i < 5; i++) {
            assertEquals(first, ShardPlan.assign(sorted, REAL_WEIGHTS, 6));
        }
    }

    // ---- degenerate inputs ----

    @Test
    @DisplayName("fewer scenarios than shards leaves the extra shards empty rather than failing")
    void moreShardsThanScenarios() {
        var sorted = names(3);
        var used = new ArrayList<Integer>();
        for (int shard = 0; shard < 8; shard++) {
            var mine = ShardPlan.shardOf(sorted, REAL_WEIGHTS, 8, shard);
            if (!mine.isEmpty()) used.add(shard);
        }
        assertEquals(3, used.size(), "three scenarios should occupy three shards");
    }

    @Test
    @DisplayName("an empty selection produces an empty plan, not an exception")
    void emptySelection() {
        assertTrue(ShardPlan.assign(List.of(), REAL_WEIGHTS, 4).isEmpty());
        assertTrue(ShardPlan.shardOf(List.of(), REAL_WEIGHTS, 4, 2).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    @DisplayName("a job count below two puts everything on shard 0")
    void serialIsShardZero(int jobs) {
        var sorted = realNames();
        assertEquals(sorted, ShardPlan.shardOf(sorted, REAL_WEIGHTS, jobs, 0));
    }

    @Test
    @DisplayName("null weights are treated as no weights")
    void nullWeights() {
        var sorted = names(8);
        assertEquals(ShardPlan.assign(sorted, Map.of(), 4), ShardPlan.assign(sorted, null, 4));
    }

    // ---- balance ----

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 6})
    @DisplayName("balancing beats round-robin, and lands near the ideal split")
    void weightedIsBetterBalancedThanRoundRobin(int jobs) {
        var sorted = realNames();
        long total = REAL_WEIGHTS.values().stream().mapToLong(Long::longValue).sum();
        long ideal = total / jobs;

        long weighted = makespan(ShardPlan.assign(sorted, REAL_WEIGHTS, jobs), jobs);
        long roundRobin = makespan(ShardPlan.assign(sorted, Map.of(), jobs), jobs);

        assertTrue(weighted <= roundRobin,
                "jobs=" + jobs + ": weighted " + weighted + "ms was worse than round-robin " + roundRobin + "ms");
        // Greedy LPT is not optimal, but on this shape it should stay within a quarter of the ideal.
        assertTrue(weighted <= ideal * 5 / 4,
                "jobs=" + jobs + ": weighted " + weighted + "ms is far off the ideal " + ideal + "ms");
    }

    /** How long the slowest shard takes, which is what a parallel run actually waits for. */
    private static long makespan(Map<String, Integer> assignment, int jobs) {
        var load = new long[jobs];
        assignment.forEach((name, shard) -> load[shard] += REAL_WEIGHTS.getOrDefault(name, 3000L));
        return java.util.Arrays.stream(load).max().orElse(0);
    }
}
