package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.ComposeElement
import com.lowdragmc.lowdraglib2.gui.ui.compose.Mount
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.asMutableState
import com.lowdragmc.lowdraglib2.gui.ui.compose.collectAsState
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.TrackData
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * The composition's own housekeeping: `/ldlib2_screen_test compose_lifecycle`.
 *
 * <p>Nothing here is a feature anyone would build a screen around. Each row is one of the ways a
 * composition meets the element tree around it — an element handed in from outside, a child added
 * by hand, a nested composition that leaves and comes back, a composition that fails — and the
 * scenario beside it checks each one still behaves.
 */
@LDLRegisterClient(name = "compose_lifecycle", registry = "ldlib2:screen_test")
class TestComposeLifecycle : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI {
        val host = ComposeElement { Lifecycle() }
        // Added by hand, after the composition's own children. The composition must leave it alone -
        // through recompositions, and through clearing its own children.
        host.addChild(TextElement().setText(Component.literal("manual")).addClass("lc-manual"))
        return ModularUI.of(UI.of(host, StylesheetManager.MODERN), entityPlayer)
    }
}

@Composable
private fun Lifecycle() {
    var ticks by remember { mutableIntStateOf(0) }
    var useB by remember { mutableStateOf(false) }
    var showInner by remember { mutableStateOf(true) }
    var explode by remember { mutableStateOf(false) }
    var clicks by remember { mutableIntStateOf(0) }

    // Built outside the composition and handed in, the way existing widgets are adopted.
    val a = remember { TextElement().setText(Component.literal("mount a")).addClass("lc-mount-a") }
    val b = remember { TextElement().setText(Component.literal("mount b")).addClass("lc-mount-b") }
    // A composition inside a composition, which leaves the tree and comes back.
    val inner = remember { ComposeElement { InnerCounter() }.addClass("lc-inner") }
    // A composition whose content always throws: it has to fail on its own, quietly.
    val broken = remember { ComposeElement { error("this content always fails, on purpose") }.addClass("lc-broken") }

    Box(layout = { width(240.px); padding { all(6.px) }; gap { all(3.px) } }, classes = "lc-root") {
        Button(onClick = { ticks++ }, text = "recompose", classes = "lc-recompose")
        Button(onClick = { useB = !useB }, text = "swap", classes = "lc-swap")
        Mount(if (useB) b else a)

        Button(onClick = { showInner = !showInner }, text = "inner", classes = "lc-toggle-inner")
        if (showInner) Mount(inner)
        Mount(broken)

        // Throws from its style block when told to. Only this node's appearance is affected.
        Box(
            layout = { size(40.px, 6.px) },
            style = {
                if (explode) error("a style block failing, on purpose")
                opacity(1f)
            },
            classes = "lc-exploding",
        )
        Button(onClick = { explode = !explode }, text = "explode", classes = "lc-explode")

        // Re-declares its handler on every recomposition; it has to stay one listener.
        Box(layout = { size(40.px, 14.px) }, events = { onMouseDown { clicks++ } }, classes = "lc-clicker")
        Text("ticks=$ticks clicks=$clicks", classes = "lc-state")

        Bridges()
        Reshuffle()
    }
}

/**
 * The state bridges, against a [TrackData] standing in for a synced value: written from the
 * composition, and written from outside it the way a sync packet would.
 */
@Composable
private fun Bridges() {
    val data = remember { TrackData(0) }
    var bridged by data.asMutableState()
    val collected by data.collectAsState()

    Row(layout = { gap { all(3.px) } }) {
        Button(onClick = { bridged += 1 }, text = "bridge +1", classes = "lc-bridge-add")
        Button(onClick = { data.value = 100 }, text = "set 100", classes = "lc-bridge-set")
    }
    Text("bridged=$bridged collected=$collected", classes = "lc-bridge-state")
}

/** A keyed list that moves, loses and gains rows in a single change. */
@Composable
private fun Reshuffle() {
    var items by remember { mutableStateOf(listOf("a", "b", "c", "d")) }

    Row(layout = { gap { all(2.px) } }, classes = "lc-items") {
        for (item in items) {
            key(item) { Text(item, classes = "lc-item lc-item-$item") }
        }
    }
    Button(onClick = { items = listOf("d", "c", "a", "e") }, text = "reshuffle", classes = "lc-reshuffle")
}

@Composable
private fun InnerCounter() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }, text = "inner $count", classes = "lc-inner-button")
}
