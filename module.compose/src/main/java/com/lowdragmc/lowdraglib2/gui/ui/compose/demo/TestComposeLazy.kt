package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyColumn
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyRow
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyVerticalGrid
import com.lowdragmc.lowdraglib2.gui.ui.compose.enabled
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.UIModifier
import com.lowdragmc.lowdraglib2.gui.ui.compose.background
import com.lowdragmc.lowdraglib2.gui.ui.compose.classes
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.gap
import com.lowdragmc.lowdraglib2.gui.ui.compose.height
import com.lowdragmc.lowdraglib2.gui.ui.compose.padding
import com.lowdragmc.lowdraglib2.gui.ui.compose.width
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * A ten-thousand row list: `/ldlib2_screen_test compose_lazy`.
 *
 * <p>Composed eagerly this would be ten thousand elements, each with a layout node and a stylesheet
 * match. The point of the demo is that scrolling it feels the same as scrolling ten rows, and that
 * selection — which is held above the list — survives rows being recycled underneath it.
 */
@LDLRegisterClient(name = "compose_lazy", registry = "ldlib2:screen_test")
class TestComposeLazy : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { LazyDemo() }
}

private const val ROW_HEIGHT = 14f

@Composable
private fun LazyDemo() {
    var count by remember { mutableStateOf(10_000) }
    // Held above the list on purpose: a row that scrolls out of view leaves the composition, and
    // anything it remembered goes with it.
    var selected by remember { mutableStateOf(-1) }

    val items = remember(count) { List(count) { "item $it" } }

    Box(UIModifier.width(240.px).padding(6.px).gap(4.px)) {
        Row(UIModifier.gap(3.px)) {
            Text(
                Component.literal("$count rows, selected $selected"),
                UIModifier.classes("lazy-status"),
            )
        }
        Button(
            onClick = { count = if (count == 10_000) 20 else 10_000 },
            modifier = UIModifier.classes("lazy-resize"),
            text = Component.literal("切换长度"),
        )

        LazyColumn(
            items = items,
            itemHeight = ROW_HEIGHT,
            modifier = UIModifier.width(220.px).height(140.px).classes("lazy-list"),
            key = { _, item -> item },
        ) { index, item ->
            Text(
                Component.literal(item),
                UIModifier.height(ROW_HEIGHT.px)
                    .classes("lazy-row")
                    .let { if (index == selected) it.background(Sprites.RECT_SOLID) else it },
            )
        }

        Button(
            onClick = { selected = 5000 },
            // Disabled once it has already been used - the flag every real UI needs and the reason
            // `enabled` is a modifier rather than a parameter on each of thirty-odd widgets.
            modifier = UIModifier.classes("lazy-select").enabled(selected != 5000),
            text = Component.literal("选中 5000"),
        )

        Grid()
        Strip()
    }
}

/** The horizontal counterpart, virtualised the same way. */
@Composable
private fun Strip() {
    val cells = remember { List(500) { "h$it" } }
    LazyRow(
        items = cells,
        itemWidth = 40f,
        modifier = UIModifier.width(220.px).height(24.px).classes("lazy-strip"),
        key = { _, cell -> cell },
    ) { _, cell ->
        Text(Component.literal(cell), UIModifier.width(40f.px).classes("strip-cell"))
    }
}

/**
 * The shape an inventory or a recipe table wants: a scrolling grid of fixed cells.
 *
 * <p>Virtualised by row rather than by cell, so a 2000-cell grid mounts a handful of rows.
 */
@Composable
private fun Grid() {
    val cells = remember { List(2000) { "c$it" } }
    LazyVerticalGrid(
        items = cells,
        columns = 8,
        cellWidth = 26f,
        cellHeight = 16f,
        modifier = UIModifier.width(220.px).height(60.px).classes("lazy-grid"),
        key = { _, cell -> cell },
    ) { _, cell ->
        Text(Component.literal(cell), UIModifier.height(16f.px).classes("grid-cell"))
    }
}
