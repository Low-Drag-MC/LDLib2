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
import com.lowdragmc.lowdraglib2.gui.ui.compose.expandVertically
import com.lowdragmc.lowdraglib2.gui.ui.compose.fadeIn
import com.lowdragmc.lowdraglib2.gui.ui.compose.fadeOut
import com.lowdragmc.lowdraglib2.gui.ui.compose.shrinkVertically
import com.lowdragmc.lowdraglib2.gui.ui.compose.slideInHorizontally
import com.lowdragmc.lowdraglib2.gui.ui.compose.slideOutHorizontally
import com.lowdragmc.lowdraglib2.gui.ui.compose.rememberInteraction
import com.lowdragmc.lowdraglib2.gui.ui.compose.track
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.gui.util.TreeNode
import com.lowdragmc.lowdraglib2.math.interpolate.Eases
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * Third demo: `/ldlib2_screen_test compose_widgets`. The split view's panes are separate
 * compositions, so state and a `CompositionLocal` read in both show that boundary is invisible.
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

        ProgressBar(
            progress = (count.coerceIn(0, 10)) / 10f,
            layout = { width(240.px); height(10.px) },
            classes = "demo-progress",
        )

        ReorderableFields()
        FadePanel()
        ExpandPanel()
        Tabs()
        ConfirmDialog()
        UnwrappedWidget()
        HoverCard()
        Remounted()
        ObservedBlock()
    }
}

/**
 * A `layout` block that reads state nothing else reads, so only the node observer re-running the
 * block can make the width follow the button.
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
    // A height given only once `wide` flips: the text must stop sizing to its content, with no recomposition.
    Text("tall", layout = { if (wide.value) height(30.px) }, classes = "observed-text")
}

/** Hover that changes *what is composed*: a `.__hovered__` stylesheet rule could not swap the caption. */
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

/** A nested [ComposeElement] taken out and put back: removal disposes its composition, so it must rebuild. */
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

/** A widget with no wrapper ([TreeList]), used directly through [UIElementNode]. */
@Composable
private fun UnwrappedWidget() {
    var depth by remember { mutableStateOf(2) }

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
        // flattenRoot has to be set before setRoot runs, so it belongs here rather than in `update`.
        factory = { TreeList<TreeNode<String, String>>().setFlattenRoot(true) },
        layout = { width(240.px); height(60.px) },
        classes = "demo-tree",
        update = {
            // Gated on `depth`, not `root`: `set` compares with `equals`, which TreeNode bases on key
            // and content but not children, so `set(root)` would never fire.
            set(depth) { setRoot(root) }
        },
    )
}

/** A modal driven by state rather than `show()`, closed from inside by its own button. */
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

/** Tabs that come, go and change places; each body is its own composition and keeps its state. */
@Composable
private fun Tabs() {
    var extra by remember { mutableStateOf(false) }
    var reversed by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var minimal by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Any>("one") }
    // Only the player's picks, and a removed selected tab handing over to its neighbour, should count.
    var reports by remember { mutableStateOf(0) }

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
        Button(
            onClick = { reversed = !reversed },
            text = Component.literal("反转页序"),
            classes = "tab-reverse",
        )
        Button(
            onClick = { hidden = !hidden },
            text = Component.literal("隐藏"),
            classes = "tab-hide",
        )
        Button(
            onClick = {
                extra = false
                selected = "one"
            },
            text = Component.literal("关第三页选第一页"),
            classes = "tab-close-pick-one",
        )
        Button(
            onClick = { minimal = !minimal },
            text = Component.literal("只留第一页"),
            classes = "tab-minimal",
        )
    }
    Text(Component.literal("selected=$selected reports=$reports"), classes = "tab-state")

    if (!hidden) {
        TabView(
            selectedKey = selected,
            onSelect = {
                reports++
                selected = it
            },
            layout = { width(240.px); height(60.px) },
            classes = "demo-tabs",
        ) {
            val pages = when {
                minimal -> listOf("one")
                extra -> listOf("one", "two", "three")
                else -> listOf("one", "two")
            }
            for (page in if (reversed) pages.reversed() else pages) {
                tab(page, page.replaceFirstChar { it.uppercase() }) {
                    Text(Component.literal("page $page"), classes = "tab-body-$page")
                }
            }
        }
    }
}

/** A panel that fades rather than blinking; on hide the content stays in the tree until the fade ends. */
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
        enter = fadeIn(TweenSpec(durationMillis = 400, easing = Eases.QUAD_OUT)),
        exit = fadeOut(TweenSpec(durationMillis = 400, easing = Eases.QUAD_OUT)),
        layout = { width(240.px) },
        classes = "fade-panel",
    ) {
        Text(Component.literal("faded in"), classes = "fade-content")
    }
}

/** A panel that slides in and out while the space it takes opens and closes, moving the text below smoothly. */
@Composable
private fun ExpandPanel() {
    var shown by remember { mutableStateOf(false) }
    val spec = TweenSpec(durationMillis = 400, easing = Eases.QUAD_OUT)

    Button(
        onClick = { shown = !shown },
        text = Component.literal("展开收起"),
        classes = "expand-toggle",
    )
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(spec) + slideInHorizontally(spec) + expandVertically(spec),
        exit = fadeOut(spec) + slideOutHorizontally(spec, targetOffsetX = 0.5f) + shrinkVertically(spec),
        layout = { width(240.px) },
        classes = "expand-panel",
    ) {
        Box(
            layout = { height(30.px); padding { all(4.px) } },
            style = { backgroundTexture(Sprites.BORDER) },
            classes = "expand-content",
        ) {
            Text(Component.literal("slides in, pushes the line below down"))
        }
    }
    Text(Component.literal("below the panel"), classes = "expand-below")
}

@Composable
private fun SplitPane(side: String, count: Int, onClick: () -> Unit) {
    Box(layout = { padding { all(3.px) }; gap { all(2.px) } }) {
        Text(Component.literal("$side ${LocalGreeting.current} $count"), classes = "pane-$side")
        Button(
            onClick = onClick,
            text = Component.literal(side),
            layout = { width(60.px) },
            classes = "pane-button-$side",
        )
    }
}

/** Keyed text fields reordered without losing focus: `reorderChildren` moves rather than detaches. */
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
