package com.lowdragmc.lowdraglib2.uitest.par;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a shard build is told to do.
 *
 * <p>A child Gradle build inherits none of the parent's project properties, so this list is the
 * <b>whole</b> of what a shard knows about the run it is part of. Anything missing from it does not
 * fail — it falls back to a default, quietly, in a process whose log nobody reads unless the run
 * goes red.
 *
 * <p>Which is exactly how {@code -PldTest} came to be missing: the parent printed the selection it
 * had been handed, every shard defaulted to {@code all}, and the only visible symptom was a
 * scenario count larger than the selection could explain.
 */
class ParallelTestOrchestratorTest {

    @Test
    @DisplayName("the selection reaches the shards, or they each run everything")
    void theSelectionIsForwarded() {
        List<String> properties =
                ParallelTestOrchestrator.childProperties("group:mine", "", 4, false);

        assertTrue(properties.contains("-PldTest=group:mine"),
                "a shard with no -PldTest defaults to 'all': " + properties);
        assertTrue(properties.contains("-PldTestJobs=4"),
                "and it has to agree with the others on how many shards there are: " + properties);
    }

    @Test
    @DisplayName("so does an exclusion, which is half of what a selection means")
    void theExclusionIsForwarded() {
        List<String> properties =
                ParallelTestOrchestrator.childProperties("all", "es_bench", 2, false);

        assertTrue(properties.contains("-PldTestExclude=es_bench"), properties.toString());
    }

    /**
     * ⚠️ Absent rather than empty. Downstream the question asked is
     * {@code project.hasProperty('ldTestExclude')}, and {@code -PldTestExclude=} answers yes to it —
     * so passing it unconditionally would turn "exclude nothing" into "exclude the empty pattern",
     * which is a different question with a different answer.
     */
    @Test
    @DisplayName("but an empty exclusion is not passed at all")
    void anEmptyExclusionIsOmitted() {
        List<String> properties =
                ParallelTestOrchestrator.childProperties("all", "", 2, false);

        assertTrue(properties.stream().noneMatch(p -> p.startsWith("-PldTestExclude")),
                properties.toString());
        assertTrue(ParallelTestOrchestrator.childProperties("all", "   ", 2, false).stream()
                .noneMatch(p -> p.startsWith("-PldTestExclude")), "blank counts as empty");
    }

    @Test
    @DisplayName("headless is only asked for when it was asked for")
    void headlessIsOptional() {
        assertFalse(ParallelTestOrchestrator.childProperties("all", "", 2, false)
                .contains("-PldTestHeadless"));
        assertTrue(ParallelTestOrchestrator.childProperties("all", "", 2, true)
                .contains("-PldTestHeadless"));
    }

    /**
     * A selection is one argument, so it needs no quoting of its own — the colon in
     * {@code group:x} and the comma in a list both have to survive intact.
     */
    @Test
    @DisplayName("a selection is passed as one argument, punctuation and all")
    void aSelectionIsOneArgument() {
        List<String> properties =
                ParallelTestOrchestrator.childProperties("a,b,group:c", "", 2, false);

        assertEquals(1, properties.stream().filter(p -> p.startsWith("-PldTest=")).count());
        assertTrue(properties.contains("-PldTest=a,b,group:c"), properties.toString());
    }
}
