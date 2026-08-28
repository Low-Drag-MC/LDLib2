package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.AnimatedVisibility
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.ComposeElement
import com.lowdragmc.lowdraglib2.gui.ui.compose.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.compose.Mount
import com.lowdragmc.lowdraglib2.gui.ui.compose.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.SplitViewHorizontal
import com.lowdragmc.lowdraglib2.gui.ui.compose.TabView
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.TextField
import com.lowdragmc.lowdraglib2.gui.ui.compose.TweenSpec
import com.lowdragmc.lowdraglib2.gui.ui.compose.UIElementNode
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.rememberInteraction
import com.lowdragmc.lowdraglib2.gui.ui.compose.track
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.gui.util.TreeNode
import com.lowdragmc.lowdraglib2.math.interpolate.Eases
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * Third demo: `/ldlib2_screen_test compose_widgets`.
 *
 * <p>Covers the two things the other two demos do not — a widget with named slots, and whether a
 * reordered row really keeps the element it had.
 *
 * <p>The split view is the interesting one. Its two panes are separate compositions, so the question
 * worth answering is whether that boundary is invisible: state read on both sides, and a
 * `CompositionLocal` provided outside it, both have to work exactly as they would in one tree.
 */
@LDLRegisterClient(name = "compose_widgets", registry = "ldlib2:screen_test")
class TestComposeWidgets : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { WidgetsDemo() }
}

/** Provided above the split, read inside both panes. */
private val LocalGreeting = compositionLocalOf { "unset" }

@Composable
private fun WidgetsDemo() {
    var count by remember { mutableStateOf(0) }

    Box(layout = { width(260.px); padding { all(6.px) }; gap { all(4.px) } }) {
        CompositionLocalProvider(LocalGreeting provides "provided") {
            SplitViewHorizontal(
                left = { SplitPane("left", count) { count++ } },
                right = { SplitPane("right", count) { count-- } },
                layout = { width(240.px); height(70.px) },
                classes = "demo-split",
            )
        }

        // Display-only widget driven straight from state.
        ProgressBar(
            progress = (count.coerceIn(0, 10)) / 10f,
            layout = { width(240.px); height(10.px) },
            classes = "demo-progress",
        )

        ReorderableFields()
        FadePanel()
        Tabs()
        ConfirmDialog()
        UnwrappedWidget()
        HoverCard()
        Remounted()
        ObservedBlock()
    }
}

/**
 * A `layout` block that reads state, where *nothing else does*.
 *
 * <p>This is the case the node observer exists for, arranged so it cannot pass by accident. Blocks
 * run in the apply phase, so their reads are invisible to the composition; `wide` is never read from
 * a composable body here, so no recomposition can be triggered by changing it. If the width follows
 * the button, it is because the observer re-ran this one block — nothing else could have.
 */
@Composable
private fun ObservedBlock() {
    val wide = remember { mutableStateOf(false) }

    Button(
        onClick = { wide.value = !wide.value },
        text = Component.literal("变宽"),
        classes = "observed-toggle",
    )
    Box(
        layout = { width(if (wide.value) 120.px else 40.px); height(8.px) },
        classes = "observed-box",
    )
}

/**
 * Hover that changes *what is composed*, not just how it is painted.
 *
 * <p>The class flip below could equally have been a `.__hovered__` stylesheet rule — core maintains
 * that class on the hovered element and its ancestors already. The caption underneath could not: a
 * stylesheet cannot swap the text, and that is what [rememberInteraction] is for.
 */
@Composable
private fun HoverCard() {
    val interaction = rememberInteraction()
    Box(
        layout = { size(60.px, 14.px) },
        classes = if (interaction.hovered) "hover-card hovered" else "hover-card",
        events = { track(interaction) },
    ) {
        Text(Component.literal(if (interaction.hovered) "hovered" else "idle"), classes = "hover-label")
    }
}

/**
 * A nested [ComposeElement] taken out of the tree and put back.
 *
 * <p>Removal runs `onRemoved`, which disposes the composition — that is what stops a closed screen
 * from being retained forever by the recomposer's global snapshot observer. Putting the element back
 * therefore has to rebuild the composition from scratch, and this is the demo that proves it does
 * rather than leaving a silently empty element behind.
 */
@Composable
private fun Remounted() {
    var mounted by remember { mutableStateOf(true) }
    val nested = remember {
        ComposeElement { Text(Component.literal("nested"), classes = "remount-body") }
    }

    Button(
        onClick = { mounted = !mounted },
        text = Component.literal("挂载/卸载"),
        classes = "remount-toggle",
    )
    if (mounted) {
        Mount(nested)
    }
}

/**
 * A widget with no wrapper at all, used directly.
 *
 * <p>[TreeList] has no `@Composable` of its own anywhere in this package — it is here precisely
 * because it does not. [UIElementNode] takes any [com.lowdragmc.lowdraglib2.gui.ui.UIElement]
 * subclass, from this mod or another, and gives it the same treatment a wrapped widget gets:
 * `factory` builds it once, `set` re-applies a property only when it changes, and the same
 * `layout` / `style` / `classes` blocks work as usual.
 *
 * <p>Which means a wrapper is sugar, not a prerequisite. Writing one is worth it for a widget used
 * often enough that naming its properties pays for itself; for everything else this is the API.
 */
@Composable
private fun UnwrappedWidget() {
    var depth by remember { mutableStateOf(2) }

    // Rebuilt whenever the state it derives from changes - exactly like any other derived value.
    val root = remember(depth) {
        TreeNode<String, String>("root").also { node ->
            repeat(depth) { i -> node.getOrCreateChild("branch $i") }
        }
    }

    Button(
        onClick = { depth = if (depth == 2) 4 else 2 },
        text = Component.literal("增删树节点"),
        classes = "tree-grow",
    )
    UIElementNode(
        // One-off construction settings belong here, not in `update`: flattenRoot has to be true
        // before setRoot runs, because that is what decides whether the root itself becomes a row.
        factory = { TreeList<TreeNode<String, String>>().setFlattenRoot(true) },
        layout = { width(240.px); height(60.px) },
        classes = "demo-tree",
        update = {
            // Gated on `depth`, not on `root` — and that distinction is the one thing to know about
            // this escape hatch. `set` decides whether to run by comparing with `equals`, and
            // TreeNode's equals looks at its key and content but not at its children: two different
            // trees rooted at "root" compare equal, so `set(root)` would silently never fire.
            // Key the update on whatever actually varies.
            set(depth) { setRoot(root) }
        },
    )
}

/**
 * A modal driven by state rather than by calling `show()`.
 *
 * <p>The case worth demonstrating is closing it from the inside: the dialog's own button sets the
 * same state the `visible` flag reads, so the two never disagree.
 */
@Composable
private fun ConfirmDialog() {
    var asking by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf("none") }

    Button(
        onClick = { asking = true },
        text = Component.literal("打开对话框"),
        classes = "dialog-open",
    )
    Text(Component.literal("answer: $answered"), classes = "dialog-answer")

    Dialog(visible = asking, onDismiss = { asking = false }) {
        Text(Component.literal("确定吗？"), classes = "dialog-body")
        Button(
            onClick = { answered = "yes"; asking = false },
            text = Component.literal("确定"),
            classes = "dialog-confirm",
        )
    }
}

/**
 * Tabs that come and go.
 *
 * <p>Each tab's body is a separate composition, and each keeps its own state — the counter in a tab
 * survives switching away and back, because the tab stays composed. Closing one has to take its
 * elements with it, which is the case that leaks if the teardown is missing.
 */
@Composable
private fun Tabs() {
    var extra by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Any>("one") }

    Row(layout = { gap { all(3.px) } }) {
        Button(
            onClick = { extra = !extra },
            text = Component.literal("增删页"),
            classes = "tab-toggle",
        )
        Button(
            onClick = { selected = "two" },
            text = Component.literal("选第二页"),
            classes = "tab-select-two",
        )
    }

    TabView(
        selectedKey = selected,
        onSelect = { selected = it },
        layout = { width(240.px); height(60.px) },
        classes = "demo-tabs",
    ) {
        tab("one", "One") { Text(Component.literal("page one"), classes = "tab-body-one") }
        tab("two", "Two") { Text(Component.literal("page two"), classes = "tab-body-two") }
        if (extra) {
            tab("three", "Three") { Text(Component.literal("page three"), classes = "tab-body-three") }
        }
    }
}

/**
 * A panel that fades rather than blinking in and out.
 *
 * <p>The interesting half is hiding: the content has to stay in the tree for the length of the fade,
 * so a plain `if (visible)` cannot express this — it would remove the element on the same frame and
 * leave nothing to animate.
 */
@Composable
private fun FadePanel() {
    var shown by remember { mutableStateOf(false) }

    Button(
        onClick = { shown = !shown },
        text = Component.literal("淡入淡出"),
        classes = "fade-toggle",
    )
    AnimatedVisibility(
        visible = shown,
        animationSpec = TweenSpec(durationMillis = 400, easing = Eases.QUAD_OUT),
        layout = { width(240.px) },
        classes = "fade-panel",
    ) {
        Text(Component.literal("faded in"), classes = "fade-content")
    }
}

@Composable
private fun SplitPane(side: String, count: Int, onClick: () -> Unit) {
    Box(layout = { padding { all(3.px) }; gap { all(2.px) } }) {
        // Both panes read the same `count` and the same local, across a composition boundary.
        Text(Component.literal("$side ${LocalGreeting.current} $count"), classes = "pane-$side")
        Button(
            onClick = onClick,
            text = Component.literal(side),
            layout = { width(60.px) },
            classes = "pane-button-$side",
        )
    }
}

/**
 * Keyed text fields that can be reordered.
 *
 * <p>The point is focus. Reordering goes through `UIElement.reorderChildren`, which moves a child
 * without detaching it; doing the same thing with remove-and-re-add would produce the same visible
 * order while dropping whatever had keyboard focus.
 */
@Composable
private fun ReorderableFields() {
    var order by remember { mutableStateOf(listOf("a", "b")) }
    var values by remember { mutableStateOf(mapOf("a" to "", "b" to "")) }

    Row(layout = { gap { all(3.px) } }) {
        order.forEach { id ->
            key(id) {
                TextField(
                    value = values[id].orEmpty(),
                    onValueChange = { values = values + (id to it) },
                    layout = { width(100.px) },
                    classes = "field field-$id",
                )
            }
        }
    }
    Button(
        onClick = { order = order.reversed() },
        text = Component.literal("交换"),
        classes = "swap-fields",
    )
}
