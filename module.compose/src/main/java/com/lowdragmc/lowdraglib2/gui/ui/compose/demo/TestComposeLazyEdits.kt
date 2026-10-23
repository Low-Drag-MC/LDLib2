package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyColumn
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyRow
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyVerticalGrid
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.world.entity.player.Player

/** Lazy containers whose data changes under them: `/ldlib2_screen_test compose_lazy_edits`. */
@LDLRegisterClient(name = "compose_lazy_edits", registry = "ldlib2:screen_test")
class TestComposeLazyEdits : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { LazyEdits() }
}

@Composable
private fun LazyEdits() {
    Row(layout = { padding { all(6.px) }; gap { all(8.px) } }, classes = "le-root") {
        Box(layout = { width(210.px); gap { all(4.px) } }) {
            Feed()
            LongFeed()
            ResizingFeed()
            Strip()
        }
        Box(layout = { width(190.px); gap { all(4.px) } }) {
            GrowingGrid()
            SavingGrid()
            ColumnsGrid()
            Duplicates()
        }
    }
}

/** A log that grows at both ends; clicking a row deletes it. The row on screen has to stay on screen. */
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
        Text(entry, layout = { height(14.px) }, classes = "le-entry", events = { onMouseDown { feed = feed - entry } })
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

/** Ten thousand rows, where one row inserted above is a tiny fraction of the scroll range. */
@Composable
private fun LongFeed() {
    var feed by remember { mutableStateOf(List(10_000) { "long $it" }) }
    var added by remember { mutableIntStateOf(0) }

    LazyColumn(
        items = feed,
        itemHeight = 14f,
        layout = { width(200.px); height(56.px) },
        classes = "le-long",
        key = { _, entry -> entry },
    ) { _, entry ->
        Text(entry, layout = { height(14.px) }, classes = "le-long-entry")
    }
    Button(onClick = { added++; feed = listOf("new $added") + feed }, text = "prepend one", classes = "le-long-prepend")
}

/** Variable-height rows that change height while the list is scrolled. */
@Composable
private fun ResizingFeed() {
    var tall by remember { mutableStateOf(false) }

    LazyColumn(
        items = remember { List(200) { "var $it" } },
        itemHeight = 14f,
        variableHeight = true,
        layout = { width(200.px); height(56.px) },
        classes = "le-var",
        key = { _, entry -> entry },
    ) { index, entry ->
        Text(entry, layout = { height(if (tall && index % 3 == 0) 30.px else 14.px) }, classes = "le-var-entry")
    }
    Button(onClick = { tall = !tall }, text = "resize", classes = "le-var-resize")
}

/** A horizontal list, which the ordinary mouse wheel has to scroll. */
@Composable
private fun Strip() {
    LazyRow(
        items = remember { List(50) { "r$it" } },
        itemWidth = 30f,
        layout = { width(120.px); height(24.px) },
        classes = "le-strip",
    ) { _, cell ->
        Text(cell, layout = { width(30.px) })
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
        layout = { width(180.px); height(56.px) },
        classes = "le-grid",
        key = { _, cell -> cell },
    ) { _, cell ->
        Text(cell, classes = "le-cell le-cell-$cell")
    }
    Button(onClick = { repeat(4) { cells.add("g${cells.size}") } }, text = "add 4", classes = "le-grow")
}

/** Cells with saved state, moved to another row by an insertion, and deleted and brought back. */
@Composable
private fun SavingGrid() {
    var cells by remember { mutableStateOf(List(8) { "s$it" }) }

    LazyVerticalGrid(
        items = cells,
        columns = 4,
        cellWidth = 40f,
        cellHeight = 14f,
        layout = { width(180.px); height(56.px) },
        classes = "le-sgrid",
        key = { _, cell -> cell },
    ) { _, cell ->
        var on by rememberSaveable { mutableStateOf(false) }
        Text(if (on) "$cell on" else cell, classes = "le-sg le-sg-$cell", events = { onMouseDown { on = !on } })
    }
    Row(layout = { gap { all(3.px) } }) {
        Button(onClick = { cells = listOf("front") + cells.filter { it != "front" } }, text = "insert", classes = "le-sg-insert")
        Button(onClick = { cells = cells - "s3" }, text = "drop s3", classes = "le-sg-drop")
        Button(onClick = { if ("s3" !in cells) cells = cells + "s3" }, text = "restore s3", classes = "le-sg-restore")
    }
}

/** A grid whose column count changes while scrolled. */
@Composable
private fun ColumnsGrid() {
    var columns by remember { mutableIntStateOf(4) }

    LazyVerticalGrid(
        items = remember { List(200) { "c$it" } },
        columns = columns,
        cellWidth = 40f,
        cellHeight = 14f,
        layout = { width(180.px); height(56.px) },
        classes = "le-cgrid",
        key = { _, cell -> cell },
    ) { _, cell ->
        Text(cell, classes = "le-cg le-cg-$cell")
    }
    Button(onClick = { columns = if (columns == 4) 2 else 4 }, text = "columns", classes = "le-cg-columns")
}

/** Keys that repeat. Unusual, and still not allowed to stop the composition. */
@Composable
private fun Duplicates() {
    var extra by remember { mutableStateOf(false) }
    val items = if (extra) listOf("same", "same", "other", "same") else listOf("same", "same", "other")

    LazyColumn(
        items = items,
        itemHeight = 14f,
        layout = { width(180.px); height(56.px) },
        classes = "le-dupes",
        key = { _, item -> item },
    ) { index, item ->
        Text("$item $index", layout = { height(14.px) }, classes = "le-dupe")
    }
    Button(onClick = { extra = !extra }, text = "one more", classes = "le-dupe-more")
}
