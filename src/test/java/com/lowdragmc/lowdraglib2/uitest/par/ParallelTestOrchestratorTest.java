package com.lowdragmc.lowdraglib2.uitest.par;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a shard build is told to do.
 *
 * <p>A child Gradle build inherits none of the parent's project properties, so this list is the
 * <b>whole</b> of what a shard knows about the run it is part of. Anything missing from it does not
 * fail — it falls back to a default, quietly, in a process whose log nobody reads unless the run
 * goes red. Both of the ones that went missing did exactly that: the selection made every shard run
 * everything, and the window made every shard lay out at half the size the serial run used.
 */
class ParallelTestOrchestratorTest {

    private static List<String> of(String... options) {
        var map = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < options.length; i += 2) {
            map.put(options[i], options[i + 1]);
        }
        return ParallelTestOrchestrator.childProperties(map, 4, false);
    }

    @Test
    @DisplayName("the selection reaches the shards, or they each run everything")
    void theSelectionIsForwarded() {
        List<String> properties = of("selection", "group:mine");

        assertTrue(properties.contains("-PldTest=group:mine"),
                "a shard with no -PldTest defaults to 'all': " + properties);
        assertTrue(properties.contains("-PldTestJobs=4"),
                "and it has to agree with the others on how many shards there are: " + properties);
    }

    /**
     * ⚠️⚠️ The viewport, which is the one whose absence does not look like a configuration problem:
     * a shard laid out at the headless default while the serial run of the same selection used the
     * window it was given, and what that produced was clicks landing beside their targets.
     */
    @Test
    @DisplayName("so do the window and everything else that decides what a shard runs in")
    void theEnvironmentIsForwarded() {
        List<String> properties = of("selection", "all", "exclude", "slow_one",
                "window", "3840x2160", "guiScale", "2",
                "inputMode", "SYNTHETIC", "watchdogSec", "180");

        assertTrue(properties.contains("-PldTestExclude=slow_one"), properties.toString());
        assertTrue(properties.contains("-PldTestWindow=3840x2160"), properties.toString());
        assertTrue(properties.contains("-PldTestGuiScale=2"), properties.toString());
        assertTrue(properties.contains("-PldTestInputMode=SYNTHETIC"), properties.toString());
        assertTrue(properties.contains("-PldTestWatchdogSec=180"), properties.toString());
    }

    /**
     * The renderer, which is not an {@code -PldTest*} flag and is forwarded for the same reason they
     * are: a shard that came up on a different backend than the serial run makes a scenario that only
     * fails on one of the two impossible to reproduce by adding {@code -PldTestJobs}.
     */
    @Test
    @DisplayName("and the graphics backend the run was pinned to")
    void theGraphicsBackendIsForwarded() {
        assertTrue(of("selection", "all", "graphicsBackend", "vulkan")
                .contains("-PgraphicsBackend=vulkan"));
        assertTrue(of("selection", "all").stream()
                .noneMatch(p -> p.startsWith("-PgraphicsBackend")),
                "an unpinned run must leave the shards on the video option, not on an empty string");
    }

    /**
     * ⚠️ Absent rather than empty. Downstream the question asked is
     * {@code project.hasProperty('ldTestExclude')}, and {@code -PldTestExclude=} answers yes to it —
     * so passing it unconditionally would turn "exclude nothing" into "exclude the empty pattern",
     * and a blank window would override nothing while looking as though it does.
     */
    @Test
    @DisplayName("but nothing is passed blank")
    void blankOptionsAreOmitted() {
        List<String> given = of("selection", "all", "exclude", "  ", "window", "");

        assertTrue(given.stream().noneMatch(p -> p.startsWith("-PldTestExclude")), given.toString());
        assertTrue(given.stream().noneMatch(p -> p.startsWith("-PldTestWindow")), given.toString());
        assertTrue(ParallelTestOrchestrator.childProperties(Map.of(), 2, false).stream()
                .noneMatch(p -> p.startsWith("-PldTestGuiScale")), "and nothing is invented");
    }

    @Test
    @DisplayName("headless is only asked for when it was asked for")
    void headlessIsOptional() {
        assertFalse(ParallelTestOrchestrator.childProperties(Map.of(), 2, false)
                .contains("-PldTestHeadless"));
        assertTrue(ParallelTestOrchestrator.childProperties(Map.of(), 2, true)
                .contains("-PldTestHeadless"));
    }

    /**
     * A selection is one argument, so it needs no quoting of its own — the colon in
     * {@code group:x} and the comma in a list both have to survive intact.
     */
    @Test
    @DisplayName("a selection is passed as one argument, punctuation and all")
    void aSelectionIsOneArgument() {
        List<String> properties = of("selection", "a,b,group:c");

        assertEquals(1, properties.stream().filter(p -> p.startsWith("-PldTest=")).count());
        assertTrue(properties.contains("-PldTest=a,b,group:c"), properties.toString());
    }
}
