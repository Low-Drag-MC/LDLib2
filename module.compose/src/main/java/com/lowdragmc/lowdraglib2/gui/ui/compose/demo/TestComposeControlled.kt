package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.compose.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.Selector
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

/** Controlled widgets where the caller says no: `/ldlib2_screen_test compose_controlled`. */
@LDLRegisterClient(name = "compose_controlled", registry = "ldlib2:screen_test")
class TestComposeControlled : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        // Escape reaches a dialog only in a UI that does not close on it first.
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { Controlled() }.shouldCloseOnEsc(false)
}

@Composable
private fun Controlled() {
    Box(layout = { width(300.px); padding { all(6.px) }; gap { all(3.px) } }, classes = "c-root") {
        Refusals()
        Ranges()
        Candidates()
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
    Row(layout = { gap { all(3.px) } }) {
        Button(onClick = { max = 1000f; level = 500f }, text = "widen", classes = "c-widen")
        // Range only: the value is clamped on the way down and comes back on the way up.
        Button(onClick = { max = 100f }, text = "shrink", classes = "c-shrink")
        Button(onClick = { max = 1000f }, text = "restore", classes = "c-restore")
    }
}

/** A selector whose candidates are a list changed in place. */
@Composable
private fun Candidates() {
    val options = remember { mutableStateListOf("x") }
    var chosen by remember { mutableStateOf<String?>("x") }
    Row(layout = { gap { all(3.px) } }) {
        Selector(chosen, { chosen = it }, options, layout = { width(60.px) }, classes = "c-selector")
        Button(onClick = { options += "y" }, text = "add y", classes = "c-add-option")
    }
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

/**
 * Toggles in a group, all driven by one value. Checking "d" is refused, and "drop picked" removes the
 * checked toggle while leaving the state naming it.
 */
@Composable
private fun Group() {
    var pick by remember { mutableStateOf("a") }
    var reports by remember { mutableIntStateOf(0) }
    var names by remember { mutableStateOf(listOf("a", "b", "c", "d")) }
    // Hoisted so unchanged toggles are skipped, leaving only the group wrapper to correct a toggle
    // the widget checked on its own.
    val labels = remember { listOf("a", "b", "c", "d").associateWith { Component.literal(it) } }

    ToggleGroup(layout = { flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW) }, classes = "c-group") {
        for (name in names) {
            key(name) {
                Toggle(
                    checked = pick == name,
                    onCheckedChange = { checked ->
                        reports++
                        if (checked && name != "d") pick = name
                    },
                    text = labels[name],
                    classes = "c-toggle-$name",
                )
            }
        }
    }
    Row(layout = { gap { all(3.px) } }) {
        Button(onClick = { pick = "c" }, text = "pick c", classes = "c-pick-c")
        Button(onClick = { names = names - pick }, text = "drop picked", classes = "c-drop-picked")
    }
    Text("pick=$pick reports=$reports", classes = "c-pick-state")
}

/** A dialog that is already open when the screen appears. */
@Composable
private fun Dialogs() {
    var open by remember { mutableStateOf(true) }
    var dismissed by remember { mutableIntStateOf(0) }
    var titled by remember { mutableStateOf(true) }

    Button(onClick = { open = true }, text = "open", classes = "c-dialog-open")
    Text("dismissed=$dismissed", classes = "c-dismissed")
    Dialog(
        visible = open,
        onDismiss = {
            dismissed++
            open = false
        },
        title = { if (titled) Text("title", classes = "c-dialog-title") },
        buttons = { Button(onClick = { open = false }, text = "close", classes = "c-dialog-close") },
        dismissOnClickOutside = true,
        layout = { width(160.px) },
        classes = "c-dialog",
    ) {
        Text("body", classes = "c-dialog-body")
        Button(onClick = { titled = !titled }, text = "title", classes = "c-dialog-retitle")
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
