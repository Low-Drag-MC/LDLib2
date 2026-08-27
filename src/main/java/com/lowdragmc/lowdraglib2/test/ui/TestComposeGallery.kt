package com.lowdragmc.lowdraglib2.test.ui

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
import com.lowdragmc.lowdraglib2.gui.ui.compose.UIModifier
import com.lowdragmc.lowdraglib2.gui.ui.compose.UITemplate
import com.lowdragmc.lowdraglib2.gui.ui.compose.classes
import com.lowdragmc.lowdraglib2.gui.ui.compose.composeModularUI
import com.lowdragmc.lowdraglib2.gui.ui.compose.gap
import com.lowdragmc.lowdraglib2.gui.ui.compose.height
import com.lowdragmc.lowdraglib2.gui.ui.compose.padding
import com.lowdragmc.lowdraglib2.gui.ui.compose.width
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient
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
    Box(UIModifier.width(300.px).padding(6.px).gap(3.px).classes("gallery-root")) {
        // Column is the one container the other demos never used.
        Column(UIModifier.gap(3.px).classes("gallery-column")) {
            Label(Component.literal("label"), UIModifier.classes("g-label"))
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
        modifier = UIModifier.classes("g-add-line"),
        text = Component.literal("加一行"),
    )
    // The array-valued widgets: their controlled-value guard has to compare contents, because
    // String[] compares by identity and would look changed on every recomposition.
    TextArea(
        lines = lines,
        onLinesChange = { lines = it },
        modifier = UIModifier.width(140.px).height(30.px).classes("g-textarea"),
    )
    CodeEditor(
        lines = code,
        onLinesChange = { code = it },
        language = Languages.JAVASCRIPT,
        modifier = UIModifier.width(140.px).height(30.px).classes("g-codeeditor"),
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
        modifier = UIModifier.classes("g-set-slider"),
        text = Component.literal("滑块 0.75"),
    )
    // Rows need their own height: a Row with no height collapses to zero and its children draw over
    // whatever comes next. Same trap as a bare Text, and the reason `Text` self-sizes by default.
    Row(UIModifier.gap(3.px).height(32.px)) {
        Slider(slider, { slider = it }, UIModifier.width(80.px).height(10.px).classes("g-slider"))
        SliderVertical(vSlider, { vSlider = it }, UIModifier.width(10.px).height(30.px).classes("g-slider-v"))
        ScrollerHorizontal(hScroll, { hScroll = it }, UIModifier.width(80.px).height(10.px).classes("g-scroller-h"))
        ScrollerVertical(vScroll, { vScroll = it }, UIModifier.width(10.px).height(30.px).classes("g-scroller-v"))
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
        modifier = UIModifier.classes("g-set-selector"),
        text = Component.literal("选 gamma"),
    )
    Button(
        onClick = { colour = 0xFFCC3333.toInt() },
        modifier = UIModifier.classes("g-set-colour"),
        text = Component.literal("换颜色"),
    )
    Selector(
        selected = picked,
        candidates = listOf("alpha", "beta", "gamma"),
        onSelectedChange = { picked = it },
        modifier = UIModifier.width(80.px).height(12.px).classes("g-selector"),
    )
    ColorSelector(colour, { colour = it }, UIModifier.width(40.px).height(12.px).classes("g-colour"))
    TagField(tag, { tag = it }, UIModifier.width(120.px).height(12.px).classes("g-tagfield"))
    StructuredTagEditor(
        structured,
        { structured = it },
        UIModifier.width(140.px).height(24.px).classes("g-structured"),
    )
    // Renders nothing; it exists to hold a value, so the only thing to check is that it composes.
    BindableValue(headless, { headless = it }, UIModifier.classes("g-headless"))
}

@Composable
private fun SlotValues() {
    var stack by remember { mutableStateOf(ItemStack(Items.APPLE, 3)) }
    val fluid = remember { FluidStack.EMPTY }

    Button(
        onClick = { stack = ItemStack(Items.DIAMOND, 7) },
        modifier = UIModifier.classes("g-set-item"),
        text = Component.literal("换物品"),
    )
    Row(UIModifier.gap(3.px).height(20.px)) {
        ItemSlot(stack, { stack = it }, UIModifier.classes("g-itemslot"))
        FluidSlot(fluid, {}, UIModifier.classes("g-fluidslot"))
        ToggleGroup(UIModifier.gap(2.px).classes("g-togglegroup")) {
            Text(Component.literal("grouped"))
        }
    }
    // Binds itself to the player inventory once attached to a menu; on a plain screen it just draws.
    InventorySlots(UIModifier.classes("g-inventory"))
}

@Composable
private fun Structural() {
    // Mount adopts an element built outside the composition. Remembered, because handing over a
    // fresh instance every recomposition would replace the node each time.
    val adopted = remember { UIElement().addClass("g-mounted") }

    SplitViewVertical(
        modifier = UIModifier.width(280.px).height(50.px).classes("g-split-v"),
        top = { Text(Component.literal("top"), UIModifier.classes("g-split-top")) },
        bottom = { Mount(adopted) },
    )
    // A null path is the "nothing selected yet" state an editor field starts in.
    UITemplate(null, UIModifier.width(60.px).height(10.px).classes("g-template"))
}
