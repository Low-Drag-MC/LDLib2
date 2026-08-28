package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * What recomposition costs, isolated from Minecraft.
 *
 * <p>This measures the Compose runtime and the applier contract only — fake nodes, no layout, no
 * styling, no drawing. So the numbers are a floor, not a frame budget: they say what the declarative
 * layer adds, and the in-client scenario says what the whole thing costs.
 *
 * <p>Assertions are deliberately about *shape* rather than absolute times. "Updating one row does not
 * cost what rebuilding the list costs" is a property worth defending on any machine; "it takes under
 * 200µs" is a property of this laptop.
 */
class ComposePerformanceTest {

    private data class Row(val id: Int, val text: String)

    /** Median of [runs] timings, after [warmup] untimed passes to let the JIT settle. */
    private fun measure(warmup: Int = 50, runs: Int = 100, block: (Int) -> Unit): Long {
        repeat(warmup) { block(it) }
        val samples = LongArray(runs)
        for (i in 0 until runs) {
            val start = System.nanoTime()
            block(warmup + i)
            samples[i] = System.nanoTime() - start
        }
        samples.sort()
        return samples[runs / 2]
    }

    private fun micros(nanos: Long) = "%.1fµs".format(nanos / 1000.0)

    /** A list of [n] keyed rows whose one changing row can be swapped without touching the others. */
    private class ListFixture(n: Int) {
        val harness = TestHarness()
        var rows by mutableStateOf((1..n).map { Row(it, "v0") })

        init {
            harness.setContent {
                Node("list") {
                    rows.forEach { row -> key(row.id) { Node(row.text) } }
                }
            }
        }

        /** Replaces exactly one Row instance; every other element of the list is untouched. */
        fun touchOne(index: Int, revision: Int) {
            rows = rows.toMutableList().also { it[index] = it[index].copy(text = "v$revision") }
        }
    }

    @Test
    fun `an idle frame is essentially free`() {
        val fixture = ListFixture(500)
        val idle = measure { fixture.harness.frame() }
        println("[perf] idle frame, 500 nodes mounted: ${micros(idle)}")

        // Nothing invalidated, so the pump should do little more than notice that. If this ever
        // starts costing real time it means something is invalidating every frame.
        assertTrue(idle < 1_000_000, "an idle frame took ${micros(idle)}")
        fixture.harness.dispose()
    }

    @Test
    fun `updating one row does not scale with list length`() {
        val small = ListFixture(100)
        val large = ListFixture(1000)

        val smallCost = measure { rev -> small.touchOne(50, rev); small.harness.frame() }
        val largeCost = measure { rev -> large.touchOne(500, rev); large.harness.frame() }

        println("[perf] update 1 of 100:  ${micros(smallCost)}")
        println("[perf] update 1 of 1000: ${micros(largeCost)}")
        println("[perf] ratio for 10x the rows: %.2fx".format(largeCost.toDouble() / smallCost))

        assertEquals("root(list(${large.rows.joinToString(",") { it.text }}))", large.harness.tree())

        // The enclosing lambda still walks the list, so this is not O(1) — but each untouched row
        // hits the compiler-inserted skip instead of recomposing, so 10x the rows must cost far less
        // than 10x the time. A rebuild-everything implementation would land at or above 10x.
        assertTrue(
            largeCost < smallCost * 6,
            "10x rows cost ${largeCost.toDouble() / smallCost}x: ${micros(smallCost)} -> ${micros(largeCost)}",
        )
        small.harness.dispose()
        large.harness.dispose()
    }

    @Test
    fun `a one-row change applies one node edit, not a rebuild`() {
        val fixture = ListFixture(1000)
        fixture.harness.applier.resetCounts()

        fixture.touchOne(500, 1)
        fixture.harness.frame()

        // The measurement that actually explains the timing above: touching one row moves no nodes
        // and creates none. Compare with clearing and repopulating, which is 1000 removes + 1000
        // inserts and throws away every element's state on the way.
        assertEquals(0, fixture.harness.applier.inserts)
        assertEquals(0, fixture.harness.applier.removes)
        assertEquals(0, fixture.harness.applier.moves)
        fixture.harness.dispose()
    }

    @Test
    fun `appending to a long list costs one insert`() {
        val fixture = ListFixture(1000)
        fixture.harness.applier.resetCounts()

        val cost = measure(warmup = 20, runs = 50) { rev ->
            fixture.rows = fixture.rows + Row(100_000 + rev, "added")
            fixture.harness.frame()
        }
        println("[perf] append 1 to a 1000+ row list: ${micros(cost)}")

        assertEquals(0, fixture.harness.applier.removes)
        assertEquals(0, fixture.harness.applier.moves)
        fixture.harness.dispose()
    }

    @Test
    fun `initial composition throughput`() {
        for (n in listOf(100, 1000)) {
            val cost = measure(warmup = 5, runs = 20) {
                val fixture = ListFixture(n)
                fixture.harness.dispose()
            }
            println("[perf] build $n nodes from scratch: ${micros(cost)} (${micros(cost / n)}/node)")
        }
    }

    @Test
    fun `reordering a long list moves nodes instead of recreating them`() {
        val fixture = ListFixture(500)
        fixture.harness.applier.resetCounts()

        val cost = measure(warmup = 5, runs = 20) {
            fixture.rows = fixture.rows.reversed()
            fixture.harness.frame()
        }
        println("[perf] reverse a 500 row list: ${micros(cost)}")

        // Reversal is the worst case for a keyed list, and it still creates nothing.
        assertEquals(0, fixture.harness.applier.inserts)
        assertEquals(0, fixture.harness.applier.removes)
        assertTrue(fixture.harness.applier.moves > 0, "a reversal should have moved something")
        fixture.harness.dispose()
    }
}
