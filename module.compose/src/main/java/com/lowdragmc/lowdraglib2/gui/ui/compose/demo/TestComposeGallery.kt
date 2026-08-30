package com.lowdragmc.lowdraglib2.gui.ui.compose.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.compose.BindableValue
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

/**
 * Every wrapper that the other demos do not reach: `/ldlib2_screen_test compose_gallery`.
 *
 * <p>Existing demos were written around whatever each was demonstrating, which left two thirds of
 * the widget wrappers compiled but never once instantiated. That is not the same as working —
 * `LazyRow` looked fine and had a wrong line in it until something actually rendered it. So this
 * screen exists to make every remaining wrapper run at least once, and the scenario beside it checks
 * the part that would realistically be wrong: the controlled-value round trip.
 */
@LDLRegisterClient(name = "compose_gallery", registry = "ldlib2:screen_test")
class TestComposeGallery : IScreenTest {
    override fun createUI(entityPlayer: Player?): ModularUI =
        composeModularUI(entityPlayer, StylesheetManager.MODERN) { Gallery() }
}

@Composable
private fun Gallery() {
    Box(layout = { width(300.px); padding { all(6.px) }; gap { all(3.px) } }, classes = "gallery-root") {
        // Column is the one container the other demos never used.
        Column(layout = { gap { all(3.px) } }, classes = "gallery-column") {
            Label(Component.literal("label"), classes = "g-label")
            TextValues()
            NumericValues()
            PickerValues()
            SlotValues()
        }
        Structural()
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
    // The array-valued widgets: their controlled-value guard has to compare contents, because
    // String[] compares by identity and would look changed on every recomposition.
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
    // Rows need their own height: a Row with no height collapses to zero and its children draw over
    // whatever comes next. Same trap as a bare Text, and the reason `Text` self-sizes by default.
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
    // Width only, no height — it is a whole picker panel (saturation square, hue strip, channel
    // rows) and sizes itself, which is how core uses it too. Pinning a height here is what made the
    // first version of this screen look broken: the panel does not clip, so the rows that did not
    // fit were drawn straight over the widgets below rather than being cropped.
    ColorSelector(colour, { colour = it }, layout = { width(90.px) }, classes = "g-colour")
    TagField(tag, { tag = it }, layout = { size(120.px, 14.px) }, classes = "g-tagfield")
    // Tall enough for its header row plus a line of tree: at 24px the rows spilled out of the
    // element and drew over the widgets underneath, because nothing here clips overflow.
    StructuredTagEditor(
        structured,
        { structured = it },
        layout = { size(140.px, 56.px) },
        classes = "g-structured",
    )
    // Renders nothing; it exists to hold a value, so the only thing to check is that it composes.
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
    // Binds itself to the player inventory once attached to a menu; on a plain screen it just draws.
    InventorySlots(classes = "g-inventory")
}

@Composable
private fun Structural() {
    // Mount adopts an element built outside the composition. Remembered, because handing over a
    // fresh instance every recomposition would replace the node each time.
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
