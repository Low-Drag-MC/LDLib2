package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
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
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/** A ten-thousand row list plus lazy rows and grids: `/ldlib2_screen_test compose_lazy`. */
@LDLRegisterClient(name = "compose_lazy", registry = "ldlib2:screen_test")
class TestComposeLazy : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { LazyDemo() }
}

private const val ROW_HEIGHT = 14f

@Composable
private fun LazyDemo() {
    var count by remember { mutableStateOf(10_000) }
    // Held above the list: a row scrolled out of view leaves the composition and forgets what it remembered.
    var selected by remember { mutableStateOf(-1) }

    val items = remember(count) { List(count) { "item $it" } }

    Row(layout = { gap { all(6.px) } }) {
        Box(layout = { width(240.px); padding { all(6.px) }; gap { all(4.px) } }) {
            Row(layout = { gap { all(3.px) } }) {
                Text(
                    Component.literal("$count rows, selected $selected"),
                    classes = "lazy-status",
                )
            }
            Button(
                onClick = { count = if (count == 10_000) 20 else 10_000 },
                text = Component.literal("切换长度"),
                classes = "lazy-resize",
            )

            LazyColumn(
                items = items,
                itemHeight = ROW_HEIGHT,
                layout = { width(220.px); height(140.px) },
                classes = "lazy-list",
                key = { _, item -> item },
            ) { index, item ->
                Text(
                    Component.literal(item),
                    layout = { height(ROW_HEIGHT.px) },
                    classes = "lazy-row",
                    // Both branches set a texture: a style block is never undone.
                    style = {
                        backgroundTexture(if (index == selected) Sprites.RECT_SOLID else IGuiTexture.EMPTY)
                    },
                )
            }

            Button(
                onClick = { selected = 5000 },
                text = Component.literal("选中 5000"),
                classes = "lazy-select",
                enabled = selected != 5000,
            )

            Grid()
            Strip()
        }
        // The variable-size lists, in their own column so the screen fits at a normal GUI scale.
        Box(layout = { width(240.px); padding { all(6.px) }; gap { all(4.px) } }) {
            Ragged()
            RaggedStrip()
            RaggedGrid()
        }
    }
}

/** Rows of differing heights, which the list learns by measuring; unmounted rows keep the 14px estimate. */
@Composable
private fun Ragged() {
    val rows = remember { List(400) { it } }
    LazyColumn(
        items = rows,
        itemHeight = 14f,
        variableHeight = true,
        layout = { width(200.px); height(80.px) },
        classes = "ragged-list",
        key = { _, row -> row },
    ) { index, row ->
        Box(
            layout = { width(180.px); height((10 + (row % 5) * 8).px) },
            classes = "ragged-row ragged-row-$index",
        )
    }
}

/** [Ragged] across: cells of five different widths, which the strip only learns by measuring them. */
@Composable
private fun RaggedStrip() {
    val cells = remember { List(300) { it } }
    LazyRow(
        items = cells,
        itemWidth = 20f,
        variableWidth = true,
        layout = { width(220.px); height(24.px) },
        classes = "ragged-strip",
        key = { _, cell -> cell },
    ) { index, cell ->
        Box(
            layout = { width((12 + (cell % 5) * 9).px); height(16.px) },
            style = { backgroundTexture(if (cell % 2 == 0) Sprites.RECT_SOLID else IGuiTexture.EMPTY) },
            classes = "ragged-cell ragged-cell-$index",
        )
    }
}

/** A grid whose rows differ in height: each row is as tall as its tallest cell. */
@Composable
private fun RaggedGrid() {
    val cells = remember { List(600) { it } }
    LazyVerticalGrid(
        items = cells,
        columns = 6,
        cellWidth = 34f,
        cellHeight = 12f,
        variableHeight = true,
        layout = { width(220.px); height(70.px) },
        classes = "ragged-grid",
        key = { _, cell -> cell },
    ) { index, cell ->
        Box(
            layout = { height((if (cell % 15 == 0) 26 else 10).px) },
            style = { backgroundTexture(if (cell % 15 == 0) Sprites.RECT_SOLID else Sprites.BORDER) },
            classes = "ragged-grid-cell ragged-grid-cell-$index",
        )
    }
}

/** The horizontal counterpart, virtualised the same way. */
@Composable
private fun Strip() {
    val cells = remember { List(500) { "h$it" } }
    LazyRow(
        items = cells,
        itemWidth = 40f,
        layout = { width(220.px); height(24.px) },
        classes = "lazy-strip",
        key = { _, cell -> cell },
    ) { _, cell ->
        Text(Component.literal(cell), layout = { width(40f.px) }, classes = "strip-cell")
    }
}

/** A scrolling grid of fixed cells, virtualised by row rather than by cell. */
@Composable
private fun Grid() {
    val cells = remember { List(2000) { "c$it" } }
    LazyVerticalGrid(
        items = cells,
        columns = 8,
        cellWidth = 26f,
        cellHeight = 16f,
        layout = { width(220.px); height(60.px) },
        classes = "lazy-grid",
        key = { _, cell -> cell },
    ) { _, cell ->
        Text(Component.literal(cell), layout = { height(16f.px) }, classes = "grid-cell")
    }
}
