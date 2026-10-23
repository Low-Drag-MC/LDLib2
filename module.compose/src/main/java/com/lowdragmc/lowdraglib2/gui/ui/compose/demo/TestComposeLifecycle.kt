package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.compose.AnimatedVisibility
import com.lowdragmc.lowdraglib2.gui.ui.compose.EnterTransition
import com.lowdragmc.lowdraglib2.gui.ui.compose.ExitTransition
import com.lowdragmc.lowdraglib2.gui.ui.compose.expandVertically
import com.lowdragmc.lowdraglib2.gui.ui.compose.fadeIn
import com.lowdragmc.lowdraglib2.gui.ui.compose.fadeOut
import com.lowdragmc.lowdraglib2.gui.ui.compose.rememberInteraction
import com.lowdragmc.lowdraglib2.gui.ui.compose.shrinkVertically
import com.lowdragmc.lowdraglib2.gui.ui.compose.track
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

/** The composition's own housekeeping against the element tree around it: `/ldlib2_screen_test compose_lifecycle`. */
@LDLRegisterClient(name = "compose_lifecycle", registry = "ldlib2:screen_test")
class TestComposeLifecycle : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI {
        val host = ComposeElement { Lifecycle() }
        // Added by hand after the composition's own children; the composition must leave it alone.
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
    // A nested composition with a manual child of its own, which leaves the tree and comes back.
    val inner = remember {
        ComposeElement { InnerCounter() }.addClass("lc-inner").also {
            it.addChild(TextElement().setText(Component.literal("inner manual")).addClass("lc-inner-manual"))
        }
    }
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
        Moves()
        Leftovers()
        Stolen()
        FirstEffect()
        Channels()
        Tracking()
    }
}

/** Movable content with children of its own, carried between parents and then edited. */
@Composable
private fun Moves() {
    var left by remember { mutableStateOf(true) }
    var extra by remember { mutableStateOf(false) }
    val movable = remember {
        movableContentOf {
            Box(classes = "lc-movable") {
                Text("m", classes = "lc-m1")
                if (extra) Text("m2", classes = "lc-m2")
            }
        }
    }
    Row(layout = { gap { all(3.px) } }) {
        Box(classes = "lc-left") { if (left) movable() }
        Box(classes = "lc-right") { if (!left) movable() }
        Button(onClick = { left = !left }, text = "move", classes = "lc-move")
        Button(onClick = { extra = !extra }, text = "extra", classes = "lc-extra")
    }
}

/** A remembered element emitted again by a new node, which must not keep its last children. */
@Composable
private fun Leftovers() {
    var open by remember { mutableStateOf(true) }
    val card = remember { UIElement().addClass("lc-card") }
    Row(layout = { gap { all(3.px) } }) {
        if (open) Mount(card) { Text("x", classes = "lc-card-x") }
        Button(onClick = { open = !open }, text = "card", classes = "lc-card-toggle")
    }
}

/** One element moving to a parent that is updated first, which takes it before the old one lets go. */
@Composable
private fun Stolen() {
    var top by remember { mutableStateOf(false) }
    val shared = remember { TextElement().setText(Component.literal("shared")).addClass("lc-shared") }
    Box(classes = "lc-top") { if (top) Mount(shared) }
    Box(classes = "lc-bottom") {
        if (!top) Mount(shared)
        Text("tail", classes = "lc-tail")
    }
    Button(onClick = { top = !top }, text = "steal", classes = "lc-steal")
}

/** What the first run of an effect sees: the composed elements already belong to the UI. */
@Composable
private fun FirstEffect() {
    var attached by remember { mutableStateOf<Boolean?>(null) }
    val probe = remember { UIElement().addClass("lc-probe") }
    Mount(probe)
    LaunchedEffect(Unit) { attached = probe.modularUI != null }
    Text("first effect attached=$attached", classes = "lc-effect")
}

/** Transitions dropped while shown: what they wrote has to go, not be overwritten with defaults. */
@Composable
private fun Channels() {
    var plain by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = true,
        enter = if (plain) EnterTransition.None else fadeIn() + expandVertically(),
        exit = if (plain) ExitTransition.None else fadeOut() + shrinkVertically(),
        classes = "lc-av",
    ) {
        Text("av", layout = { height(10.px) })
    }
    Button(onClick = { plain = !plain }, text = "plain", classes = "lc-plain")
}

/** Pressed state while the pointer moves from the tracked element onto its own child. */
@Composable
private fun Tracking() {
    val interaction = rememberInteraction()
    Box(layout = { padding { all(6.px) } }, events = { track(interaction) }, classes = "lc-track") {
        Text("inside", classes = "lc-track-child")
    }
    Text("pressed=${interaction.pressed}", classes = "lc-pressed")
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
