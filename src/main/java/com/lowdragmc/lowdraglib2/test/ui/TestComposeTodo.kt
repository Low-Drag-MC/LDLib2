package com.lowdragmc.lowdraglib2.test.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.ScrollView
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.TextField
import com.lowdragmc.lowdraglib2.gui.ui.compose.Toggle
import com.lowdragmc.lowdraglib2.gui.ui.compose.UIModifier
import com.lowdragmc.lowdraglib2.gui.ui.compose.classes
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.flex
import com.lowdragmc.lowdraglib2.gui.ui.compose.gap
import com.lowdragmc.lowdraglib2.gui.ui.compose.height
import com.lowdragmc.lowdraglib2.gui.ui.compose.padding
import com.lowdragmc.lowdraglib2.gui.ui.compose.size
import com.lowdragmc.lowdraglib2.gui.ui.compose.width
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * A deliberately non-trivial demo: `/ldlib2_screen_test compose_todo`.
 *
 * <p>Where `TestCompose` shows one list changing, this puts the pieces together the way a real screen
 * would — a controlled text input, a keyed list whose contents are *derived* rather than stored, a
 * filter that changes which subset is mounted, per-row state that has to survive filtering, and
 * counters computed from the same source of truth. The interesting part is that nothing here
 * coordinates any of that: there is no refresh method, no dirty flag, and no place where the code
 * decides which elements to destroy.
 */
@LDLRegisterClient(name = "compose_todo", registry = "ldlib2:screen_test")
class TestComposeTodo : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { TodoApp() }
}

private data class Todo(val id: Int, val text: String, val done: Boolean)

private enum class Filter(val label: String) { ALL("全部"), ACTIVE("未完成"), DONE("已完成") }

@Composable
private fun TodoApp() {
    var todos by remember { mutableStateOf(emptyList<Todo>()) }
    var nextId by remember { mutableStateOf(1) }
    var draft by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.ALL) }

    // Derived, not stored. There is no second copy of the list to keep in step, so "the filter
    // changed" and "an item was ticked" are the same kind of event as far as the UI is concerned.
    val visible by remember {
        derivedStateOf {
            todos.filter {
                when (filter) {
                    Filter.ALL -> true
                    Filter.ACTIVE -> !it.done
                    Filter.DONE -> it.done
                }
            }
        }
    }
    val remaining by remember { derivedStateOf { todos.count { !it.done } } }

    fun add() {
        val text = draft.trim()
        if (text.isEmpty()) return
        todos = todos + Todo(nextId++, text, done = false)
        draft = ""
    }

    Box(UIModifier.width(240.px).padding(6.px).gap(4.px)) {
        Row(UIModifier.gap(3.px)) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = UIModifier.flex(1f).classes("todo-input"),
            )
            Button(
                onClick = { add() },
                modifier = UIModifier.width(40.px).classes("todo-add"),
                text = Component.literal("+"),
            )
        }

        Row(UIModifier.gap(3.px)) {
            Filter.entries.forEach { candidate ->
                key(candidate) {
                    Button(
                        onClick = { filter = candidate },
                        // The selected filter carries an extra class, so the modifier has to be
                        // removed again when the selection moves on.
                        modifier = UIModifier.width(60.px)
                            .classes("todo-filter", "todo-filter-${candidate.name.lowercase()}")
                            .let { if (candidate == filter) it.classes("selected") else it },
                        text = Component.literal(candidate.label),
                    )
                }
            }
        }

        Text(Component.literal("剩余 $remaining / ${todos.size}"), UIModifier.classes("todo-count"))

        ScrollView(UIModifier.height(120.px).gap(2.px)) {
            if (visible.isEmpty()) {
                Text(Component.literal("空"), UIModifier.classes("todo-empty"))
            } else {
                visible.forEach { todo ->
                    // Keyed by id, so filtering out an item and bringing it back does not shuffle
                    // any other row's element - and a row keeps its own element across filter
                    // changes rather than being rebuilt.
                    key(todo.id) {
                        TodoRow(
                            todo = todo,
                            onToggle = { done ->
                                todos = todos.map { if (it.id == todo.id) it.copy(done = done) else it }
                            },
                            onDelete = { todos = todos.filter { it.id != todo.id } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    // The per-id class is what makes an individual row addressable from a selector, for the test
    // and for anyone styling one specific row from a stylesheet.
    Row(UIModifier.gap(3.px).classes("todo-row", "todo-row-${todo.id}")) {
        // Width only: the widget lays itself out as a row of [box | label] with its own height, so
        // forcing a square would squash the box and push the label over the text beside it.
        Toggle(
            checked = todo.done,
            onCheckedChange = onToggle,
            modifier = UIModifier.width(14.px).classes("todo-toggle"),
        )
        Text(
            Component.literal(todo.text),
            UIModifier.flex(1f)
                .classes("todo-text")
                .let { if (todo.done) it.classes("done") else it },
        )
        Button(
            onClick = onDelete,
            modifier = UIModifier.size(14.px).classes("todo-delete"),
            text = Component.literal("x"),
        )
    }
}
