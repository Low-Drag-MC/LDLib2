package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Updater
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView
import com.lowdragmc.lowdraglib2.gui.ui.elements.StructuredTagEditor
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch
import com.lowdragmc.lowdraglib2.gui.ui.elements.TagField
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle
import com.lowdragmc.lowdraglib2.gui.ui.elements.ToggleGroupElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.UITemplateElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.ILanguageDefinition
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button as LdlButton
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog as LdlDialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor as LdlCodeEditor

// region containers

/** A plain container. Children stack in a column, which is LDLib2's default flex direction. */
@Composable
fun Box(
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    UIElementNode(
        factory = { UIElement() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        content = content,
    )
}

/** Children top to bottom. */
@Composable
fun Column(
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    UIElementNode(
        // At DEFAULT origin, so a caller's own `layout = { flexDirection(…) }` still wins.
        factory = {
            UIElement().apply {
                defaultLayout {
                    flexDirection(FlexDirection.COLUMN)
                    alignItems(AlignItems.FLEX_START)
                }
            }
        },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        content = content,
    )
}

/** Children left to right. */
@Composable
fun Row(
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    UIElementNode(
        factory = {
            UIElement().apply {
                defaultLayout {
                    flexDirection(FlexDirection.ROW)
                    alignItems(AlignItems.FLEX_START)
                }
            }
        },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        content = content,
    )
}

/**
 * A scrolling container.
 *
 * <p>Children go into the view container rather than onto the [ScrollerView] itself — that is what
 * [UIElement.getContentHost] arranges — so they scroll instead of sitting alongside the scrollbars.
 */
@Composable
fun ScrollView(
    scrollerStyle: (ScrollerView.ScrollerViewStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    UIElementNode(
        factory = { ScrollerView() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = scrollerStyle?.let { block -> { getScrollerViewStyle().apply(block) } },
        content = content,
    )
}

// endregion

// region text & buttons

/**
 * Remembers whether adaptive sizing is currently on, so the write only happens when it flips.
 *
 * <p>Whether a text element may size itself depends on what the caller's `layout` block wrote, so it
 * can only be decided during apply — too late for [Updater.set], which is the usual way to make a
 * write conditional. Doing it unconditionally instead costs a style write per text element per apply,
 * and that is a per-row cost in exactly the lists this layer exists to make cheap.
 */
private class AdaptiveGate {
    private var last: Boolean? = null

    fun changed(value: Boolean): Boolean {
        if (last == value) return false
        last = value
        return true
    }
}

/**
 * Text sized to its content, unless the caller sized it themselves.
 *
 * <p>`adaptive-height` is off by default on a bare [TextElement], which leaves it zero-height — fine
 * for a hand-built layout that assigns sizes anyway, wrong for `Text("hi")`, which would draw over
 * whatever came after it. So it is switched on here, and switched off again the moment the caller's
 * own `layout` block supplies a height. That check has to happen *after* the block has run, which is
 * why it lives in `reconcile` rather than being decided up front.
 */
@Composable
fun Text(
    text: Component,
    textStyle: (TextElement.TextStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val gate = remember { AdaptiveGate() }
    UIElementNode(
        factory = { TextElement() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = textStyle?.let { block -> { getTextStyle().apply(block) } },
        update = {
            set(text) { setText(it) }
            // Left unobserved on purpose: this is the wrapper's own default, not a caller's block,
            // and it reads the element rather than any composition state.
            reconcile {
                // No layout block means the caller cannot have set a height, so the common case
                // never touches the style bag at all.
                val adaptive = layout == null || !callerSet(LayoutProperties.HEIGHT)
                if (gate.changed(adaptive)) getTextStyle().asDefault { adaptiveHeight(adaptive) }
            }
        },
    )
}

@Composable
fun Text(
    text: String,
    textStyle: (TextElement.TextStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = Text(
    Component.literal(text), textStyle, layout, style, classes, id,
    enabled, visible, focusable, events,
)

/** Like [Text], but bindable — use it where a data source is going to drive the content. */
@Composable
fun Label(
    text: Component,
    textStyle: (TextElement.TextStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val gate = remember { AdaptiveGate() }
    UIElementNode(
        factory = { Label() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = textStyle?.let { block -> { getTextStyle().apply(block) } },
        update = {
            set(text) { setText(it) }
            reconcile {
                // No layout block means the caller cannot have set a height, so the common case
                // never touches the style bag at all.
                val adaptive = layout == null || !callerSet(LayoutProperties.HEIGHT)
                if (gate.changed(adaptive)) getTextStyle().asDefault { adaptiveHeight(adaptive) }
            }
        },
    )
}

/**
 * @param onClick fires on mouse-down and plays the click sound, matching
 *   [com.lowdragmc.lowdraglib2.gui.ui.elements.Button]'s own behaviour. For a press-and-release
 *   click, use `events = { onClick { … } }` on a [Box] instead.
 * @param text the button's own label. Leave it null and pass [content] to put arbitrary elements
 *   inside instead.
 * @param buttonStyle the widget's own per-state textures, with the same receiver
 *   `button.buttonStyle { }` gives you in Java.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    text: Component? = null,
    buttonStyle: (LdlButton.ButtonStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    // The listener is registered once and reads through the holder, so a fresh lambda every
    // recomposition does not mean a fresh registration every recomposition.
    val holder = remember { EventHandlerHolder() }
    holder.handler = { onClick() }
    UIElementNode(
        factory = { LdlButton().also { it.setOnClick(holder) } },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = buttonStyle?.let { block -> { getButtonStyle().apply(block) } },
        update = { set(text) { if (it == null) noText() else setText(it) } },
        content = content,
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    text: String,
    buttonStyle: (LdlButton.ButtonStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) = Button(
    onClick, Component.translatable(text), buttonStyle, layout, style, classes, id,
    enabled, visible, focusable, events, content,
)

// endregion

// region value widgets

/**
 * The shared shape of every controlled widget here: the composition owns the value, the element
 * reports edits back through [onValueChange].
 *
 * <p>Two guards keep that from looping. Writes into the element pass `notify = false`, so pushing
 * state down does not read back as a user edit; and the write is skipped entirely when the element
 * already holds that value, so a round trip settles instead of oscillating.
 */
@Composable
private fun <E : BindableUIElement<V>, V> BindableNode(
    factory: () -> E,
    value: V,
    onValueChange: (V) -> Unit,
    layout: LayoutBlock?,
    style: StyleBlock?,
    classes: String?,
    id: String?,
    enabled: Boolean,
    visible: Boolean,
    focusable: Boolean?,
    events: (EventScope.() -> Unit)?,
    widgetStyle: (E.() -> Unit)? = null,
    update: Updater<E>.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val holder = remember { ValueChangeHolder<V>() }
    holder.onValueChange = onValueChange
    UIElementNode(
        // The subscription lives exactly as long as the element does, and the element is discarded
        // wholesale when the node leaves the composition — so there is nothing to unsubscribe.
        factory = { factory().also { e -> e.registerValueListener { v -> holder.onValueChange?.invoke(v) } } },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = widgetStyle,
        update = {
            set(value) { if (getValue() != it) setValue(it, false) }
            update()
        },
        content = content,
    )
}

private class ValueChangeHolder<V> {
    var onValueChange: ((V) -> Unit)? = null
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    fieldStyle: (TextField.TextFieldStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    { TextField() }, value, onValueChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = fieldStyle?.let { block -> { getTextFieldStyle().apply(block) } },
)

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchStyle: (Switch.SwitchStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    { Switch() }, checked, onCheckedChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = switchStyle?.let { block -> { getSwitchStyle().apply(block) } },
)

/**
 * @param text the label beside the box. Defaults to none — the underlying widget ships with the
 *   literal caption "Toggle", which is a reasonable placeholder for an editor palette and never what
 *   a caller wants.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: Component? = null,
    toggleStyle: (Toggle.ToggleStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) = BindableNode(
    factory = { Toggle() },
    value = checked,
    onValueChange = onCheckedChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = toggleStyle?.let { block -> { getToggleStyle().apply(block) } },
    update = { set(text) { toggleLabel.setText(it ?: Component.empty()) } },
    content = content,
)

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    sliderStyle: (Slider.SliderStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    factory = { Slider.Horizontal() },
    value = value,
    onValueChange = onValueChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = sliderStyle?.let { block -> { getSliderStyle().apply(block) } },
    update = { set(range) { setRange(it.start, it.endInclusive) } },
)

@Composable
fun SliderVertical(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    sliderStyle: (Slider.SliderStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    factory = { Slider.Vertical() },
    value = value,
    onValueChange = onValueChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = sliderStyle?.let { block -> { getSliderStyle().apply(block) } },
    update = { set(range) { setRange(it.start, it.endInclusive) } },
)

/** A draggable scrollbar as a value in its own right, for driving something by hand. */
@Composable
fun ScrollerHorizontal(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    scrollerStyle: (Scroller.ScrollerStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    factory = { Scroller.Horizontal() },
    value = value,
    onValueChange = onValueChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = scrollerStyle?.let { block -> { getScrollerStyle().apply(block) } },
    update = { set(range) { setRange(it.start, it.endInclusive) } },
)

@Composable
fun ScrollerVertical(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    scrollerStyle: (Scroller.ScrollerStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    factory = { Scroller.Vertical() },
    value = value,
    onValueChange = onValueChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = scrollerStyle?.let { block -> { getScrollerStyle().apply(block) } },
    update = { set(range) { setRange(it.start, it.endInclusive) } },
)

@Composable
fun <T> Selector(
    selected: T?,
    candidates: List<T>,
    onSelectedChange: (T?) -> Unit,
    selectorStyle: (Selector<T?>.SelectorStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode<Selector<T?>, T?>(
    // Selector<T?> rather than Selector<T>: "nothing selected" is a real state here, and the
    // element's own value type has to admit it for the controlled-value round trip to typecheck.
    factory = { Selector<T?>() },
    value = selected,
    onValueChange = onSelectedChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = selectorStyle?.let { block -> { getSelectorStyle().apply(block) } },
    update = { set(candidates) { setCandidates(it) } },
)

@Composable
fun ItemSlot(
    item: ItemStack,
    onItemChange: (ItemStack) -> Unit = {},
    slotStyle: (ItemSlot.SlotStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    { ItemSlot() }, item, onItemChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = slotStyle?.let { block -> { getSlotStyle().apply(block) } },
)

@Composable
fun FluidSlot(
    fluid: FluidStack,
    onFluidChange: (FluidStack) -> Unit = {},
    slotStyle: (FluidSlot.SlotStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    { FluidSlot() }, fluid, onFluidChange,
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = slotStyle?.let { block -> { getSlotStyle().apply(block) } },
)

@Composable
fun ColorSelector(
    color: Int,
    onColorChange: (Int) -> Unit,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode(
    { ColorSelector() }, color, onColorChange,
    layout, style, classes, id, enabled, visible, focusable, events,
)

@Composable
fun TagField(
    tag: Tag?,
    onTagChange: (Tag?) -> Unit,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode<TagField, Tag?>(
    { TagField() }, tag, onTagChange,
    layout, style, classes, id, enabled, visible, focusable, events,
)

/**
 * A headless value holder.
 *
 * <p>Renders nothing. Its use is as somewhere to hang a synced binding without that binding having
 * to belong to a visible widget — the placeholder pattern the sync tests use.
 */
@Composable
fun <T> BindableValue(
    value: T,
    onValueChange: (T) -> Unit,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode<BindableValue<T>, T>(
    { BindableValue() }, value, onValueChange,
    layout, style, classes, id, enabled, visible, focusable, events,
)

@Composable
fun StructuredTagEditor(
    tag: Tag?,
    onTagChange: (Tag?) -> Unit,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = BindableNode<StructuredTagEditor, Tag?>(
    { StructuredTagEditor() }, tag, onTagChange,
    layout, style, classes, id, enabled, visible, focusable, events,
)

/**
 * Multi-line text.
 *
 * <p>Takes a `List<String>` rather than the widget's own `String[]`, which matters for more than
 * ergonomics: arrays compare by identity, so the controlled-value guard would think the value had
 * changed on every single recomposition and write over whatever the user was typing.
 */
@Composable
fun TextArea(
    lines: List<String>,
    onLinesChange: (List<String>) -> Unit,
    areaStyle: (TextArea.TextAreaStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val holder = remember { ValueChangeHolder<List<String>>() }
    holder.onValueChange = onLinesChange
    UIElementNode(
        factory = {
            TextArea().also { area ->
                area.registerValueListener { v -> holder.onValueChange?.invoke(v?.toList().orEmpty()) }
            }
        },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = areaStyle?.let { block -> { getTextAreaStyle().apply(block) } },
        update = {
            set(lines) { next -> if (getValue()?.toList() != next) setValue(next.toTypedArray(), false) }
        },
    )
}

/** [TextArea] with syntax highlighting. */
@Composable
fun CodeEditor(
    lines: List<String>,
    onLinesChange: (List<String>) -> Unit,
    language: ILanguageDefinition,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val holder = remember { ValueChangeHolder<List<String>>() }
    holder.onValueChange = onLinesChange
    UIElementNode(
        factory = {
            LdlCodeEditor().also { editor ->
                editor.registerValueListener { v -> holder.onValueChange?.invoke(v?.toList().orEmpty()) }
            }
        },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        update = {
            set(language) { setLanguage(it) }
            set(lines) { next -> if (getValue()?.toList() != next) setValue(next.toTypedArray(), false) }
        },
    )
}

/**
 * A progress indicator.
 *
 * <p>Display-only — unlike the widgets above it is not a `BindableUIElement` and has no user input
 * to report back, so there is no `onValueChange` half.
 *
 * @param text the caption drawn over the bar. Defaults to none; the bare widget carries the
 *   placeholder text a [Label] starts with, which is never what a caller wants here.
 */
@Composable
fun ProgressBar(
    progress: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    text: Component? = null,
    barStyle: (ProgressBar.ProgressBarStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    UIElementNode(
        factory = { ProgressBar() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = barStyle?.let { block -> { getProgressBarStyle().apply(block) } },
        update = {
            set(range) { setRange(it.start, it.endInclusive) }
            set(progress) { setProgress(it) }
            set(text) { label.setText(it ?: Component.empty()) }
        },
        content = content,
    )
}

/**
 * Two panes with a draggable divider between them.
 *
 * <p>Each pane is its own [Subcompose], because the widget has two child lists and an applier only
 * knows one. Everything else behaves normally — state, `CompositionLocal`s and recomposition all
 * cross the pane boundary as if it were not there.
 */
@Composable
fun SplitViewHorizontal(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val view = remember { SplitView.Horizontal() }
    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    )
    Subcompose(view.first, left)
    Subcompose(view.second, right)
}

@Composable
fun SplitViewVertical(
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val view = remember { SplitView.Vertical() }
    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    )
    Subcompose(view.first, top)
    Subcompose(view.second, bottom)
}

/** Embeds a UI saved by the editor. */
@Composable
fun UITemplate(
    templatePath: IResourcePath?,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    UIElementNode(
        factory = { UITemplateElement() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        update = { set(templatePath) { setTemplate(it) } },
    )
}

/**
 * A modal dialog, driven by [visible] rather than by calling `show()`.
 *
 * <p>Two details make this work. The content is composed into the dialog *before* it is shown,
 * because `show()` decides whether to display its title, content and button rows from whether they
 * already have children — populate it afterwards and it appears empty. And the dialog can be closed
 * from inside itself, by its own buttons or by the overlay, so [onDismiss] reports that back; a
 * caller that ignores it will find [visible] and the dialog disagreeing.
 *
 * ```kotlin
 * var confirming by remember { mutableStateOf(false) }
 * Button(onClick = { confirming = true }, text = Component.literal("Delete"))
 * Dialog(visible = confirming, onDismiss = { confirming = false }) {
 *     Text(Component.literal("Are you sure?"))
 * }
 * ```
 */
@Composable
fun Dialog(
    visible: Boolean,
    onDismiss: () -> Unit = {},
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    content: @Composable () -> Unit,
) {
    val parent = LocalUIElement.current
    val observer = LocalNodeObserver.current
    val dialog = remember { LdlDialog() }
    val applied = remember { AppliedProps<LdlDialog>() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // Before the show effect below, so its SideEffect runs first and the content is in place by the
    // time `show()` inspects the containers.
    Subcompose(dialog.contentContainer, content)
    SideEffect {
        applied.apply(
            observer, dialog, layout, style, widget = null,
            classes = classes, id = id, enabled = true, visible = true, focusable = null, events = null,
        )
    }

    DisposableEffect(dialog) {
        dialog.setOnClose { currentOnDismiss() }
        onDispose { dialog.setOnClose(null) }
    }
    DisposableEffect(visible, parent) {
        if (visible) dialog.show(parent)
        onDispose { if (visible) dialog.close() }
    }
}

/** The player's inventory and hotbar. Binds itself once the UI is attached to a menu. */
@Composable
fun InventorySlots(
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    UIElementNode(
        factory = { InventorySlots() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    )
}

/** Groups the [Toggle]s composed inside it, so at most one is on at a time. */
@Composable
fun ToggleGroup(
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    UIElementNode(
        factory = { ToggleGroupElement() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        content = content,
    )
}

// endregion
