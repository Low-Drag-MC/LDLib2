package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.LDLib2
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.ScrollView
import com.lowdragmc.lowdraglib2.gui.ui.compose.Switch
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.TextField
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/** Demo screen for the declarative layer: `/ldlib2_screen_test compose`. */
@LDLRegisterClient(name = "compose", registry = "ldlib2:screen_test")
class TestCompose : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) {
            Demo()
        }
}

@Composable
private fun Demo() {
    Box(layout = { width(220.px); padding { all(6.px) }; gap { all(4.px) } }) {
        Text(Component.literal("Compose demo"))
        DynamicButtonList()
        Echo()
        Highlightable()
    }
}

/**
 * The two ways to make appearance follow state: `classes` is diffed, but a `style` / `textStyle`
 * block is never undone, so a property set under an `if` needs a value in both branches.
 */
@Composable
private fun Highlightable() {
    var hot by remember { mutableStateOf(false) }

    Button(
        onClick = { hot = !hot },
        text = Component.literal("高亮"),
        classes = "highlight-toggle",
    )
    Text(
        Component.literal("styled text"),
        classes = if (hot) "styled-text hot" else "styled-text",
        textStyle = {
            fontSize(if (hot) 18f else 9f)
            textColor(if (hot) 0xFFFF5555.toInt() else 0xFFFFFFFF.toInt())
        },
    )
}

@Composable
private fun DynamicButtonList() {
    var items by remember { mutableStateOf(listOf("A", "B", "C")) }

    ScrollView(layout = { height(120.px) }, contentLayout = { gap { all(3.px) } }) {
        items.forEach { item ->
            key(item) {
                Row(layout = { gap { all(3.px) } }) {
                    Button(
                        onClick = { LDLib2.LOGGER.info("clicked {}", item) },
                        text = Component.literal(item),
                        layout = { width(120.px) },
                        classes = "item-button",
                    )
                    Button(
                        onClick = { items = items - item },
                        text = Component.literal("x"),
                        layout = { size(20.px) },
                        classes = "remove-button",
                    )
                }
            }
        }
    }

    Row(layout = { gap { all(3.px) } }) {
        Button(
            onClick = { items = items + "Button ${items.size + 1}" },
            text = Component.literal("添加按钮"),
            classes = "add-button",
        )
        // Keyed rows are moved, not rebuilt, so they keep their focus and running animations.
        Button(
            onClick = { items = items.reversed() },
            text = Component.literal("反转"),
            classes = "reverse-button",
        )
        Button(
            onClick = { items = listOf("A", "B", "C") },
            text = Component.literal("重置"),
            classes = "reset-button",
        )
    }
}

/** A controlled input plus a conditional subtree — the `v-if` half of the story. */
@Composable
private fun Echo() {
    var text by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(true) }

    Row(layout = { gap { all(4.px) } }) {
        Switch(checked = shown, onCheckedChange = { shown = it }, layout = { size(14.px) })
        TextField(value = text, onValueChange = { text = it }, layout = { width(120.px) })
    }
    if (shown && text.isNotEmpty()) {
        Text(Component.literal("echo: $text"))
    }
}
