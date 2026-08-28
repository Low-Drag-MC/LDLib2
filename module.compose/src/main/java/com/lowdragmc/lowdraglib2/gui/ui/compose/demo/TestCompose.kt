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
import com.lowdragmc.lowdraglib2.gui.ui.compose.UIModifier
import com.lowdragmc.lowdraglib2.gui.ui.compose.classes
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.fontSize
import com.lowdragmc.lowdraglib2.gui.ui.compose.gap
import com.lowdragmc.lowdraglib2.gui.ui.compose.height
import com.lowdragmc.lowdraglib2.gui.ui.compose.padding
import com.lowdragmc.lowdraglib2.gui.ui.compose.size
import com.lowdragmc.lowdraglib2.gui.ui.compose.style
import com.lowdragmc.lowdraglib2.gui.ui.compose.width
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * Demo screen for the declarative layer: `/ldlib2_screen_test compose`.
 *
 * <p>The point of the list below is what is *not* in it. Adding an item changes a list and nothing
 * else — no `clearAllChildren()`, no rebuild method, no dirty flag polled on tick, and no bookkeeping
 * to put the text back in the field afterwards. Compare with the 31 `clearAllChildren()` call sites
 * elsewhere in the codebase, most of which are doing exactly that by hand.
 */
@LDLRegisterClient(name = "compose", registry = "ldlib2:screen_test")
class TestCompose : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) {
            Demo()
        }
}

@Composable
private fun Demo() {
    Box(UIModifier.width(220.px).padding(6.px).gap(4.px)) {
        Text(Component.literal("Compose demo"))
        DynamicButtonList()
        Echo()
        Highlightable()
    }
}

/**
 * Exercises the modifier diff in both directions.
 *
 * <p>Everything else in this demo has a fixed modifier chain per call site, so ops only ever get
 * applied. Here turning the highlight off has to actively take them back off again — a style
 * property the composition stops asking for has to stop being set, or it lingers forever.
 */
@Composable
private fun Highlightable() {
    var hot by remember { mutableStateOf(false) }

    Button(
        onClick = { hot = !hot },
        modifier = UIModifier.classes("highlight-toggle"),
        text = Component.literal("高亮"),
    )
    Text(
        Component.literal("styled text"),
        modifier = UIModifier.classes("styled-text").let {
            if (hot) {
                // Two shapes on purpose: the typed sugar, and the generic `style(property, value)`
                // escape hatch that reaches any property at all. Both produce the same StyleOp.
                it.classes("hot").fontSize(18f).style(PropertyRegistry.TEXT_COLOR, 0xFFFF5555.toInt())
            } else {
                it
            }
        },
    )
}

@Composable
private fun DynamicButtonList() {
    var items by remember { mutableStateOf(listOf("A", "B", "C")) }

    ScrollView(UIModifier.height(120.px).gap(3.px)) {
        items.forEach { item ->
            // `key` is optional - without it the list still works, it just matches by position, so a
            // reorder would rebuild rows instead of moving them.
            key(item) {
                Row(UIModifier.gap(3.px)) {
                    Button(
                        onClick = { LDLib2.LOGGER.info("clicked {}", item) },
                        modifier = UIModifier.width(120.px).classes("item-button"),
                        text = Component.literal(item),
                    )
                    Button(
                        onClick = { items = items - item },
                        modifier = UIModifier.size(20.px).classes("remove-button"),
                        text = Component.literal("x"),
                    )
                }
            }
        }
    }

    Row(UIModifier.gap(3.px)) {
        Button(
            onClick = { items = items + "Button ${items.size + 1}" },
            modifier = UIModifier.classes("add-button"),
            text = Component.literal("添加按钮"),
        )
        // With `key(item)` above, reversing produces moves rather than rebuilds: the very same
        // Button instances end up in the new order, keeping their focus and any running animation.
        Button(
            onClick = { items = items.reversed() },
            modifier = UIModifier.classes("reverse-button"),
            text = Component.literal("反转"),
        )
        Button(
            onClick = { items = listOf("A", "B", "C") },
            modifier = UIModifier.classes("reset-button"),
            text = Component.literal("重置"),
        )
    }
}

/** A controlled input plus a conditional subtree — the `v-if` half of the story. */
@Composable
private fun Echo() {
    var text by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(true) }

    Row(UIModifier.gap(4.px)) {
        Switch(checked = shown, onCheckedChange = { shown = it }, modifier = UIModifier.size(14.px))
        TextField(value = text, onValueChange = { text = it }, modifier = UIModifier.width(120.px))
    }
    if (shown && text.isNotEmpty()) {
        Text(Component.literal("echo: $text"))
    }
}
