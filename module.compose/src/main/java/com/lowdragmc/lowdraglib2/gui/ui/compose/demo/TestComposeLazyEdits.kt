package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyColumn
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyVerticalGrid
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.world.entity.player.Player

/**
 * Lazy containers whose data changes under them: `/ldlib2_screen_test compose_lazy_edits`.
 *
 * <p>Scrolling a fixed list is covered by `compose_lazy`. Here the list itself changes — rows added
 * above and below what is on screen, a grid fed from a list mutated in place, keys that repeat.
 */
@LDLRegisterClient(name = "compose_lazy_edits", registry = "ldlib2:screen_test")
class TestComposeLazyEdits : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { LazyEdits() }
}

@Composable
private fun LazyEdits() {
    Box(layout = { width(240.px); padding { all(6.px) }; gap { all(4.px) } }, classes = "le-root") {
        Feed()
        GrowingGrid()
        Duplicates()
    }
}

/** A log that grows at both ends. The row on screen has to stay on screen. */
@Composable
private fun Feed() {
    var feed by remember { mutableStateOf(List(100) { "entry $it" }) }
    var prepended by remember { mutableStateOf(0) }

    LazyColumn(
        items = feed,
        itemHeight = 14f,
        layout = { width(200.px); height(70.px) },
        classes = "le-feed",
        key = { _, entry -> entry },
    ) { _, entry ->
        Text(entry, layout = { height(14.px) }, classes = "le-entry")
    }
    Row(layout = { gap { all(3.px) } }) {
        Button(onClick = { feed = feed + List(100) { "entry ${100 + it}" } }, text = "append", classes = "le-append")
        Button(
            onClick = {
                prepended++
                feed = List(20) { "early ${prepended}-$it" } + feed
            },
            text = "prepend",
            classes = "le-prepend",
        )
    }
}

/** A grid backed by a snapshot list that is added to in place - the same instance throughout. */
@Composable
private fun GrowingGrid() {
    val cells = remember { mutableStateListOf<String>().apply { addAll(List(10) { "g$it" }) } }

    LazyVerticalGrid(
        items = cells,
        columns = 4,
        cellWidth = 40f,
        cellHeight = 14f,
        layout = { width(180.px); height(70.px) },
        classes = "le-grid",
        key = { _, cell -> cell },
    ) { _, cell ->
        Text(cell, classes = "le-cell le-cell-$cell")
    }
    Button(onClick = { repeat(4) { cells.add("g${cells.size}") } }, text = "add 4", classes = "le-grow")
}

/** Keys that repeat. Unusual, and still not allowed to stop the composition. */
@Composable
private fun Duplicates() {
    var extra by remember { mutableStateOf(false) }
    val items = if (extra) listOf("same", "same", "other", "same") else listOf("same", "same", "other")

    LazyColumn(
        items = items,
        itemHeight = 14f,
        layout = { width(200.px); height(60.px) },
        classes = "le-dupes",
        key = { _, item -> item },
    ) { index, item ->
        Text("$item $index", layout = { height(14.px) }, classes = "le-dupe")
    }
    Button(onClick = { extra = !extra }, text = "one more", classes = "le-dupe-more")
}
