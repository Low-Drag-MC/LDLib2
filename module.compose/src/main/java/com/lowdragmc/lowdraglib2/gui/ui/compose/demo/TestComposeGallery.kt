package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.compose.BindableValue
import net.minecraft.client.Minecraft
import com.lowdragmc.lowdraglib2.gui.util.TreeNode
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D
import com.lowdragmc.lowdraglib2.gui.ui.compose.TreeList
import com.lowdragmc.lowdraglib2.gui.ui.compose.TextField
import com.lowdragmc.lowdraglib2.gui.ui.compose.TabView
import com.lowdragmc.lowdraglib2.gui.ui.compose.Switch
import com.lowdragmc.lowdraglib2.gui.ui.compose.SplitViewHorizontal
import com.lowdragmc.lowdraglib2.gui.ui.compose.SearchComponent
import com.lowdragmc.lowdraglib2.gui.ui.compose.ScrollView
import com.lowdragmc.lowdraglib2.gui.ui.compose.Scene
import com.lowdragmc.lowdraglib2.gui.ui.compose.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.compose.Inspector
import com.lowdragmc.lowdraglib2.gui.ui.compose.GraphView
import com.lowdragmc.lowdraglib2.gui.ui.compose.Box
import com.lowdragmc.lowdraglib2.gui.ui.compose.Button
import com.lowdragmc.lowdraglib2.gui.ui.compose.CodeEditor
import com.lowdragmc.lowdraglib2.gui.ui.compose.ColorSelector
import com.lowdragmc.lowdraglib2.gui.ui.compose.Column
import com.lowdragmc.lowdraglib2.gui.ui.compose.FluidSlot
import com.lowdragmc.lowdraglib2.gui.ui.compose.InventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.compose.ItemSlot
import com.lowdragmc.lowdraglib2.gui.ui.compose.Label
import com.lowdragmc.lowdraglib2.gui.ui.compose.Mount
import com.lowdragmc.lowdraglib2.gui.ui.compose.Row
import com.lowdragmc.lowdraglib2.gui.ui.compose.ScrollerHorizontal
import com.lowdragmc.lowdraglib2.gui.ui.compose.ScrollerVertical
import com.lowdragmc.lowdraglib2.gui.ui.compose.Selector
import com.lowdragmc.lowdraglib2.gui.ui.compose.Slider
import com.lowdragmc.lowdraglib2.gui.ui.compose.SliderVertical
import com.lowdragmc.lowdraglib2.gui.ui.compose.SplitViewVertical
import com.lowdragmc.lowdraglib2.gui.ui.compose.StructuredTagEditor
import com.lowdragmc.lowdraglib2.gui.ui.compose.TagField
import com.lowdragmc.lowdraglib2.gui.ui.compose.Text
import com.lowdragmc.lowdraglib2.gui.ui.compose.TextArea
import com.lowdragmc.lowdraglib2.gui.ui.compose.Toggle
import com.lowdragmc.lowdraglib2.gui.ui.compose.ToggleGroup
import com.lowdragmc.lowdraglib2.gui.ui.compose.UITemplate
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.neoforge.fluids.FluidStack

/** Every wrapper that the other demos do not reach: `/ldlib2_screen_test compose_gallery`. */
@LDLRegisterClient(name = "compose_gallery", registry = "ldlib2:screen_test")
class TestComposeGallery : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { Gallery() }
}

@Composable
private fun Gallery() {
    Row(layout = { padding { all(6.px) }; gap { all(8.px) } }, classes = "gallery-root") {
        Box(layout = { width(300.px); gap { all(3.px) } }) {
            Column(layout = { gap { all(3.px) } }, classes = "gallery-column") {
                Label(Component.literal("label"), classes = "g-label")
                TextValues()
                NumericValues()
                PickerValues()
                SlotValues()
            }
            Structural()
        }
        Box(layout = { width(160.px) }) { Catalogue() }
    }
}

@Composable
private fun TextValues() {
    var lines by remember { mutableStateOf(listOf("one", "two")) }
    var code by remember { mutableStateOf(listOf("var x = 1")) }

    Button(
        onClick = { lines = lines + "three" },
        text = Component.literal("加一行"),
        classes = "g-add-line",
    )
    // Array-valued: the controlled-value guard must compare contents, as String[] compares by identity.
    TextArea(
        lines = lines,
        onLinesChange = { lines = it },
        layout = { size(140.px, 30.px) },
        classes = "g-textarea",
    )
    CodeEditor(
        lines = code,
        onLinesChange = { code = it },
        language = Languages.JAVASCRIPT,
        layout = { size(140.px, 30.px) },
        classes = "g-codeeditor",
    )
}

@Composable
private fun NumericValues() {
    var slider by remember { mutableStateOf(0.25f) }
    var vSlider by remember { mutableStateOf(0.5f) }
    var hScroll by remember { mutableStateOf(0f) }
    var vScroll by remember { mutableStateOf(0f) }

    Button(
        onClick = { slider = 0.75f },
        text = Component.literal("滑块 0.75"),
        classes = "g-set-slider",
    )
    // A Row with no height collapses to zero and its children draw over whatever comes next.
    Row(layout = { gap { all(3.px) }; height(32.px) }) {
        Slider(slider, { slider = it }, layout = { size(80.px, 10.px) }, classes = "g-slider")
        SliderVertical(vSlider, { vSlider = it }, layout = { size(10.px, 30.px) }, classes = "g-slider-v")
        ScrollerHorizontal(hScroll, { hScroll = it }, layout = { size(80.px, 10.px) }, classes = "g-scroller-h")
        ScrollerVertical(vScroll, { vScroll = it }, layout = { size(10.px, 30.px) }, classes = "g-scroller-v")
    }
}

@Composable
private fun PickerValues() {
    var picked by remember { mutableStateOf<String?>("beta") }
    var colour by remember { mutableStateOf(0xFF3366CC.toInt()) }
    var tag by remember { mutableStateOf<Tag?>(StringTag.valueOf("hello")) }
    var structured by remember { mutableStateOf<Tag?>(CompoundTag()) }
    var headless by remember { mutableStateOf("headless") }

    Button(
        onClick = { picked = "gamma" },
        text = Component.literal("选 gamma"),
        classes = "g-set-selector",
    )
    Button(
        onClick = { colour = 0xFFCC3333.toInt() },
        text = Component.literal("换颜色"),
        classes = "g-set-colour",
    )
    Selector(
        selected = picked,
        candidates = listOf("alpha", "beta", "gamma"),
        onSelectedChange = { picked = it },
        layout = { size(80.px, 12.px) },
        classes = "g-selector",
    )
    // Width only: the picker panel sizes itself and does not clip, so a pinned height spills its
    // rows over the widgets below.
    ColorSelector(colour, { colour = it }, layout = { width(90.px) }, classes = "g-colour")
    TagField(tag, { tag = it }, layout = { size(120.px, 14.px) }, classes = "g-tagfield")
    // Tall enough for its header row plus a line of tree; nothing here clips overflow.
    StructuredTagEditor(
        structured,
        { structured = it },
        layout = { size(140.px, 56.px) },
        classes = "g-structured",
    )
    // Renders nothing; only checked to compose.
    BindableValue(headless, { headless = it }, classes = "g-headless")
}

@Composable
private fun SlotValues() {
    var stack by remember { mutableStateOf(ItemStack(Items.APPLE, 3)) }
    val fluid = remember { FluidStack.EMPTY }

    Button(
        onClick = { stack = ItemStack(Items.DIAMOND, 7) },
        text = Component.literal("换物品"),
        classes = "g-set-item",
    )
    Row(layout = { gap { all(3.px) }; height(20.px) }) {
        ItemSlot(stack, { stack = it }, classes = "g-itemslot")
        FluidSlot(fluid, {}, classes = "g-fluidslot")
        ToggleGroup(layout = { gap { all(2.px) } }, classes = "g-togglegroup") {
            Text(Component.literal("grouped"))
        }
    }
    // Display-only on a plain screen; in a menu it refuses to bind.
    InventorySlots(classes = "g-inventory")
}

@Composable
private fun Structural() {
    // Remembered: a fresh instance every recomposition would replace the node each time.
    val adopted = remember { UIElement().addClass("g-mounted") }

    SplitViewVertical(
        top = { Text(Component.literal("top"), classes = "g-split-top") },
        bottom = { Mount(adopted) },
        layout = { size(280.px, 50.px) },
        classes = "g-split-v",
    )
    // A null path is the "nothing selected yet" state an editor field starts in.
    UITemplate(null, layout = { size(60.px, 10.px) }, classes = "g-template")
}

/**
 * The second column: every registered widget the first one does not show. `ComposeGalleryScenario`
 * checks both columns against the widget registry.
 */
@Composable
private fun Catalogue() {
    Column(layout = { gap { all(3.px) } }, classes = "gallery-catalogue") {
        Basics()
        Containers()
        Larger()
    }
}

@Composable
private fun Basics() {
    var text by remember { mutableStateOf("typed") }
    var on by remember { mutableStateOf(true) }
    var ticked by remember { mutableStateOf(false) }

    Text("text", classes = "g-text")
    Button(onClick = {}, text = "button", classes = "g-button")
    TextField(text, { text = it }, layout = { width(100.px) }, classes = "g-textfield")
    Row(layout = { gap { all(3.px) } }) {
        Switch(on, { on = it }, classes = "g-switch")
        Toggle(ticked, { ticked = it }, text = Component.literal("toggle"), classes = "g-toggle")
    }
    ProgressBar(0.4f, layout = { size(100.px, 8.px) }, classes = "g-progress")
}

@Composable
private fun Containers() {
    var tab by remember { mutableStateOf<Any>("a") }

    ScrollView(layout = { size(120.px, 30.px) }, classes = "g-scrollview") {
        repeat(5) { Text("row $it") }
    }
    TabView(selectedKey = tab, onSelect = { tab = it }, layout = { size(140.px, 40.px) }, classes = "g-tabview") {
        tab("a", "A") { Text("tab a") }
        tab("b", "B") { Text("tab b") }
    }
    SplitViewHorizontal(
        left = { Text("left") },
        right = { Text("right") },
        layout = { size(140.px, 20.px) },
        classes = "g-split-h",
    )
}

@Composable
private fun Larger() {
    val tree = remember {
        TreeNode<String, String>("root").also { root ->
            root.getOrCreateChild("leaf a")
            root.getOrCreateChild("leaf b")
        }
    }
    var picked by remember { mutableStateOf(emptySet<TreeNode<String, String>>()) }
    var found by remember { mutableStateOf<String?>(null) }
    val inspected = remember { Transform2D() }
    val player = Minecraft.getInstance().player
    val below = remember { player?.blockPosition()?.below() }

    TreeList(
        root = tree,
        selected = picked,
        onSelectedChange = { picked = it },
        flattenRoot = true,
        layout = { size(140.px, 30.px) },
        classes = "g-tree",
    )
    SearchComponent(
        selected = found,
        onSelectedChange = { found = it },
        search = { query, found -> listOf("apple", "apricot", "banana").filter { it.startsWith(query) }.forEach(found) },
        layout = { size(100.px, 14.px) },
        classes = "g-search",
    )
    Inspector(target = inspected, layout = { size(140.px, 40.px) }, classes = "g-inspector")
    Scene(
        world = player?.level(),
        blocks = listOfNotNull(below),
        layout = { size(60.px, 40.px) },
        classes = "g-scene",
    )
    GraphView(layout = { size(140.px, 40.px) }, classes = "g-graph") {
        Box(layout = { size(20.px, 10.px) }, style = { backgroundTexture(Sprites.RECT_SOLID) }, classes = "g-graph-node")
    }
}
