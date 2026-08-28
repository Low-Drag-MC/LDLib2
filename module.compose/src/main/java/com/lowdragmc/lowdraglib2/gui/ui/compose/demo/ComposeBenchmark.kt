package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.compose.ComposeHost
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.UIModifier
import com.lowdragmc.lowdraglib2.gui.ui.compose.classes
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement
import net.minecraft.network.chat.Component

/**
 * What the declarative layer costs against a real element tree.
 *
 * <p>The pure-JVM benchmarks measure the Compose runtime with fake nodes. This measures the part
 * they cannot: every insert here registers the element with a live {@link ModularUI}, allocates a
 * taffy layout node and runs stylesheet matching against it. That is where the real time goes, and
 * it is also what makes the comparison at the end meaningful — the imperative baseline pays exactly
 * the same per-element costs, so the difference is purely how many elements each approach touches.
 *
 * <p>Driven directly rather than through a rendered screen: [ComposeHost.onFrame] is the same call
 * the frame callback makes, so pumping it in a loop measures the same work without the noise of
 * everything else in a frame.
 */
object ComposeBenchmark {

    private data class Row(val id: Int, val text: String)

    private fun median(samples: LongArray): Long {
        samples.sort()
        return samples[samples.size / 2]
    }

    private fun ms(nanos: Long) = "%.2fms".format(nanos / 1_000_000.0)

    private fun measure(warmup: Int, runs: Int, block: (Int) -> Unit): Long {
        repeat(warmup) { block(it) }
        val samples = LongArray(runs)
        for (i in 0 until runs) {
            val start = System.nanoTime()
            block(warmup + i)
            samples[i] = System.nanoTime() - start
        }
        return median(samples)
    }

    /** A detached-but-registered UI, so element attach/detach pays its real price. */
    private fun newUI(): Pair<ModularUI, UIElement> {
        val root = UIElement()
        val ui = ModularUI.of(UI.of(root))
        ui.init(400, 400)
        return ui to root
    }

    @JvmStatic
    fun run(size: Int = 500): List<String> {
        val report = mutableListOf<String>()
        report += "rows = $size"

        // --- build from scratch -------------------------------------------------------------
        val buildCost = measure(warmup = 2, runs = 5) {
            val (_, root) = newUI()
            val host = ComposeHost(root)
            var rows by mutableStateOf((1..size).map { Row(it, "row $it") })
            host.setContent {
                rows.forEach { row -> key(row.id) { Text(Component.literal(row.text), UIModifier.classes("bench-row")) } }
            }
            host.dispose()
        }
        report += "build $size rows          : ${ms(buildCost)}"

        // --- steady state -------------------------------------------------------------------
        val (_, liveRoot) = newUI()
        val liveHost = ComposeHost(liveRoot)
        var liveRows by mutableStateOf((1..size).map { Row(it, "row $it") })
        liveHost.setContent {
            liveRows.forEach { row -> key(row.id) { Text(Component.literal(row.text), UIModifier.classes("bench-row")) } }
        }

        val idleCost = measure(warmup = 20, runs = 50) { liveHost.onFrame(it.toLong()) }
        report += "idle frame               : ${ms(idleCost)}"

        val updateCost = measure(warmup = 10, runs = 30) { rev ->
            liveRows = liveRows.toMutableList().also { it[size / 2] = it[size / 2].copy(text = "upd $rev") }
            liveHost.onFrame(rev.toLong())
        }
        report += "update 1 of $size         : ${ms(updateCost)}"

        val appendCost = measure(warmup = 5, runs = 20) { rev ->
            liveRows = liveRows + Row(1_000_000 + rev, "added")
            liveHost.onFrame(rev.toLong())
        }
        report += "append 1 row             : ${ms(appendCost)}"

        val reverseCost = measure(warmup = 2, runs = 5) { rev ->
            liveRows = liveRows.reversed()
            liveHost.onFrame(rev.toLong())
        }
        report += "reverse $size rows        : ${ms(reverseCost)}"
        liveHost.dispose()

        // --- the same update, with nothing allocated per row --------------------------------
        // Identical structure, except each row carries a ready-made Component and they all share
        // one modifier instance. The skip still has to be *reached*, so the enclosing loop still
        // walks every row - but it now walks them without building two objects per row on the way.
        class StableRow(val id: Int, val text: Component)

        val sharedModifier = UIModifier.classes("bench-row")
        val (_, stableRoot) = newUI()
        val stableHost = ComposeHost(stableRoot)
        var stableRows by mutableStateOf(
            (1..size).map { StableRow(it, Component.literal("row $it")) }
        )
        stableHost.setContent {
            stableRows.forEach { row -> key(row.id) { Text(row.text, sharedModifier) } }
        }
        val stableUpdateCost = measure(warmup = 10, runs = 30) { rev ->
            stableRows = stableRows.toMutableList()
                .also { it[size / 2] = StableRow(it[size / 2].id, Component.literal("upd $rev")) }
            stableHost.onFrame(rev.toLong())
        }
        report += "update 1 of $size (hoisted): ${ms(stableUpdateCost)}"
        stableHost.dispose()

        // --- the thing this replaces --------------------------------------------------------
        // clearAllChildren + rebuild, the shape used at 31 call sites across the codebase. Same
        // per-element costs, but it touches every row to change one.
        val (_, imperativeRoot) = newUI()
        repeat(size) { imperativeRoot.addChild(TextElement().setText(Component.literal("row $it")).addClass("bench-row")) }
        val rebuildCost = measure(warmup = 2, runs = 5) { rev ->
            imperativeRoot.clearAllChildren()
            repeat(size) { imperativeRoot.addChild(TextElement().setText(Component.literal("row $it rev$rev")).addClass("bench-row")) }
        }
        report += "clear+rebuild $size rows  : ${ms(rebuildCost)}   <- the imperative baseline"
        report += "update speedup vs rebuild: %.0fx".format(rebuildCost.toDouble() / updateCost)

        return report
    }
}
