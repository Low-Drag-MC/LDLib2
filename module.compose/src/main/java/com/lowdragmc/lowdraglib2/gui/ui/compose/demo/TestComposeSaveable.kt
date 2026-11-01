package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.LazyColumn
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.TabView
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * State that outlives the screen: `/ldlib2_screen_test compose_saveable`, then close it and open it
 * again.
 */
@LDLRegisterClient(name = "compose_saveable", registry = "ldlib2:screen_test")
class TestComposeSaveable : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN, stateKey = STATE_KEY) { SaveableDemo() }

    companion object {
        const val STATE_KEY = "ldlib2_compose:saveable_demo"
    }
}

@Composable
private fun SaveableDemo() {
    var kept by rememberSaveable { mutableStateOf(0) }
    var lost by remember { mutableStateOf(0) }
    var showB by remember { mutableStateOf(true) }
    val rows = remember { List(200) { it } }

    Box(layout = { width(240.px); padding { all(6.px) }; gap { all(4.px) } }) {
        Row(layout = { gap { all(3.px) } }) {
            Button(
                onClick = { kept++ },
                text = Component.literal("saveable $kept"),
                layout = { width(100.px) },
                classes = "kept",
            )
            Button(
                onClick = { lost++ },
                text = Component.literal("remember $lost"),
                layout = { width(100.px) },
                classes = "lost",
            )
        }

        LazyColumn(
            items = rows,
            itemHeight = 14f,
            layout = { width(220.px); height(70.px) },
            classes = "starred-list",
            key = { _, row -> row },
        ) { _, row ->
            // A plain remember would be dropped when the row scrolls out.
            var starred by rememberSaveable { mutableStateOf(false) }
            Button(
                onClick = { starred = !starred },
                text = Component.literal(if (starred) "* row $row" else "row $row"),
                layout = { height(14.px) },
                classes = "star-row star-row-$row",
            )
        }

        Button(
            onClick = { showB = !showB },
            text = Component.literal("增删页 B"),
            classes = "toggle-b",
        )
        TabView(layout = { width(220.px); height(50.px) }, classes = "saveable-tabs") {
            tab("a", "A") { TabCounter("a") }
            if (showB) tab("b", "B") { TabCounter("b") }
        }
    }
}

@Composable
private fun TabCounter(name: String) {
    var count by rememberSaveable { mutableStateOf(0) }
    Button(
        onClick = { count++ },
        text = Component.literal("$name $count"),
        classes = "tab-counter-$name",
    )
}
