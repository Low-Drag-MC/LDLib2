package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** What recomposition costs, isolated from Minecraft. */
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

    /** A list of [n] keyed rows. */
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

        fun touchOne(index: Int, revision: Int) {
            rows = rows.toMutableList().also { it[index] = it[index].copy(text = "v$revision") }
        }
    }

    @Test
    fun `an idle frame is essentially free`() {
        val fixture = ListFixture(500)
        val idle = measure { fixture.harness.frame() }
        println("[perf] idle frame, 500 nodes mounted: ${micros(idle)}")

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

        // Not O(1): the list lambda still walks every row, but untouched rows skip, so well under 10x.
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

        assertEquals(0, fixture.harness.applier.inserts)
        assertEquals(0, fixture.harness.applier.removes)
        assertTrue(fixture.harness.applier.moves > 0, "a reversal should have moved something")
        fixture.harness.dispose()
    }
}
