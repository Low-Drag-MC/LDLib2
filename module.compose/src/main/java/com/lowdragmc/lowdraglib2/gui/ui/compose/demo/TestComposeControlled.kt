package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.compose.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.ScrollView
import com.lowdragmc.lowdraglib2.gui.ui.compose.Slider
import com.lowdragmc.lowdraglib2.gui.ui.compose.Switch
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.TextArea
import com.lowdragmc.lowdraglib2.gui.ui.compose.TextField
import com.lowdragmc.lowdraglib2.gui.ui.compose.Toggle
import com.lowdragmc.lowdraglib2.gui.ui.compose.ToggleGroup
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * Controlled widgets where the caller says no: `/ldlib2_screen_test compose_controlled`.
 *
 * <p>Each row is one of the things a controlled widget has to get right besides following state —
 * an edit the caller refuses, a value that only makes sense against a range given alongside it, a
 * group whose side effects are not edits, a dialog that starts open. The scenario beside it drives
 * each one.
 */
@LDLRegisterClient(name = "compose_controlled", registry = "ldlib2:screen_test")
class TestComposeControlled : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { Controlled() }
}

@Composable
private fun Controlled() {
    Box(layout = { width(300.px); padding { all(6.px) }; gap { all(3.px) } }, classes = "c-root") {
        Refusals()
        Ranges()
        Labels()
        Group()
        Dialogs()
        Containers()
    }
}

/** A field that refuses edits in its callback, one with a validator, and an area that normalises. */
@Composable
private fun Refusals() {
    var short by remember { mutableStateOf("abc") }
    var digits by remember { mutableStateOf("12") }
    var three by remember { mutableStateOf("") }
    var shout by remember { mutableStateOf(listOf("")) }

    Row(layout = { gap { all(3.px) } }) {
        // At most five characters, enforced by simply not storing anything longer.
        TextField(
            value = short,
            onValueChange = { if (it.length <= 5) short = it },
            layout = { width(90.px) },
            classes = "c-short",
        )
        TextField(
            value = digits,
            onValueChange = { digits = it },
            charFilter = Char::isDigit,
            layout = { width(90.px) },
            classes = "c-digits",
        )
        TextField(
            value = three,
            onValueChange = { three = it },
            validator = { text -> text.length <= 3 },
            layout = { width(60.px) },
            classes = "c-three",
        )
    }
    Text("short=$short digits=$digits three=$three", classes = "c-text-state")
    // Upper-cases whatever is typed: a correction the caller applies on every edit.
    TextArea(
        lines = shout,
        onLinesChange = { lines -> shout = lines.map { it.uppercase() } },
        layout = { width(200.px); height(30.px) },
        classes = "c-shout",
    )
}

/** A slider whose range and value change together. */
@Composable
private fun Ranges() {
    var level by remember { mutableStateOf(50f) }
    var max by remember { mutableStateOf(100f) }

    Slider(
        value = level,
        onValueChange = { level = it },
        range = 0f..max,
        layout = { width(200.px) },
        classes = "c-slider",
    )
    ProgressBar(progress = level, range = 0f..max, layout = { width(200.px); height(8.px) }, classes = "c-progress")
    Button(onClick = { max = 1000f; level = 500f }, text = "widen", classes = "c-widen")
}

/** A button whose label comes and goes, a toggle with none, and a switch. */
@Composable
private fun Labels() {
    var busy by remember { mutableStateOf(false) }
    var on by remember { mutableStateOf(false) }
    var bare by remember { mutableStateOf(false) }

    Row(layout = { gap { all(3.px) } }) {
        Button(
            onClick = { busy = !busy },
            text = if (busy) null else Component.literal("ready"),
            layout = { width(60.px); height(14.px) },
            classes = "c-busy",
        )
        Toggle(checked = bare, onCheckedChange = { bare = it }, classes = "c-bare-toggle")
        Switch(checked = on, onCheckedChange = { on = it }, classes = "c-switch")
    }
    Text("on=$on", classes = "c-on")
}

/** Three toggles in a group, all driven by one value. */
@Composable
private fun Group() {
    var pick by remember { mutableStateOf("a") }
    var reports by remember { mutableIntStateOf(0) }

    ToggleGroup(layout = { flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW) }, classes = "c-group") {
        for (key in listOf("a", "b", "c")) {
            Toggle(
                checked = pick == key,
                onCheckedChange = { checked ->
                    reports++
                    if (checked) pick = key
                },
                text = Component.literal(key),
                classes = "c-toggle-$key",
            )
        }
    }
    Button(onClick = { pick = "c" }, text = "pick c", classes = "c-pick-c")
    Text("pick=$pick reports=$reports", classes = "c-pick-state")
}

/** A dialog that is already open when the screen appears. */
@Composable
private fun Dialogs() {
    var open by remember { mutableStateOf(true) }
    var dismissed by remember { mutableIntStateOf(0) }

    Button(onClick = { open = true }, text = "open", classes = "c-dialog-open")
    Text("dismissed=$dismissed", classes = "c-dismissed")
    Dialog(
        visible = open,
        onDismiss = {
            dismissed++
            open = false
        },
        title = { Text("title", classes = "c-dialog-title") },
        buttons = { Button(onClick = { open = false }, text = "close", classes = "c-dialog-close") },
        dismissOnClickOutside = true,
        layout = { width(160.px) },
        classes = "c-dialog",
    ) {
        Text("body", classes = "c-dialog-body")
    }
}

/** Row spacing that belongs to the scrolled content, not to the scroll view. */
@Composable
private fun Containers() {
    ScrollView(
        layout = { width(120.px); height(40.px) },
        contentLayout = { gap { all(5.px) } },
        classes = "c-scroll",
    ) {
        Text("one", classes = "c-row c-row-1")
        Text("two", classes = "c-row c-row-2")
    }
}
