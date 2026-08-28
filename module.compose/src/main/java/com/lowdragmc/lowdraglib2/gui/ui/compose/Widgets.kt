package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Updater
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector
import com.lowdragmc.lowdraglib2.gui.ui.elements.StructuredTagEditor
import com.lowdragmc.lowdraglib2.gui.ui.elements.UITemplateElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.ILanguageDefinition
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog as LdlDialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor as LdlCodeEditor
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch
import com.lowdragmc.lowdraglib2.gui.ui.elements.TagField
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle
import com.lowdragmc.lowdraglib2.gui.ui.elements.ToggleGroupElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots
import net.minecraft.nbt.Tag
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties
import com.lowdragmc.lowdraglib2.gui.ui.style.Property
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button as LdlButton

// region containers

/** A plain container. Children stack in a column, which is LDLib2's default flex direction. */
@Composable
fun Box(modifier: UIModifier = UIModifier, content: @Composable () -> Unit = {}) {
    UIElementNode(factory = { UIElement() }, modifier = modifier, content = content)
}

/** Children top to bottom. */
@Composable
fun Column(modifier: UIModifier = UIModifier, content: @Composable () -> Unit = {}) {
    UIElementNode(
        factory = { UIElement() },
        modifier = UIModifier.flexDirection(FlexDirection.COLUMN)
            .alignItems(AlignItems.FLEX_START)
            .then(modifier),
        content = content,
    )
}

/** Children left to right. */
@Composable
fun Row(modifier: UIModifier = UIModifier, content: @Composable () -> Unit = {}) {
    UIElementNode(
        factory = { UIElement() },
        modifier = UIModifier.flexDirection(FlexDirection.ROW)
            .alignItems(AlignItems.FLEX_START)
            .then(modifier),
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
fun ScrollView(modifier: UIModifier = UIModifier, content: @Composable () -> Unit = {}) {
    UIElementNode(factory = { ScrollerView() }, modifier = modifier, content = content)
}

// endregion

// region text & buttons

/**
 * Whether the chain sets [property] itself.
 *
 * <p>Used to decide whether a widget may size itself. Text elements do that by writing their
 * measured size at `IMPORTANT` origin, which out-ranks the `INLINE` origin modifiers write at — so a
 * widget that self-sizes unconditionally would silently ignore `UIModifier.height(...)`.
 */
private fun UIModifier.sets(property: Property<*>): Boolean =
    ops.any { it is StyleOp<*> && it.property == property }

/**
 * Text sized to its content, unless the chain says otherwise.
 *
 * <p>`adaptive-height` is off by default on a bare [TextElement], which leaves it zero-height —
 * fine for a hand-built layout that assigns sizes anyway, wrong for `Text("hi")`, which would draw
 * over whatever came after it. So it is enabled here, and disabled again the moment a caller
 * supplies a height of their own.
 */
@Composable
fun Text(text: Component, modifier: UIModifier = UIModifier) {
    val adaptiveHeight = !modifier.sets(LayoutProperties.HEIGHT)
    UIElementNode(
        factory = { TextElement() },
        modifier = modifier,
        update = {
            set(text) { setText(it) }
            set(adaptiveHeight) { adaptive -> textStyle { it.adaptiveHeight(adaptive) } }
        },
    )
}

@Composable
fun Text(text: String, modifier: UIModifier = UIModifier) = Text(Component.literal(text), modifier)

/** Like [Text], but bindable — use it where a data source is going to drive the content. */
@Composable
fun Label(text: Component, modifier: UIModifier = UIModifier) {
    val adaptiveHeight = !modifier.sets(LayoutProperties.HEIGHT)
    UIElementNode(
        factory = { Label() },
        modifier = modifier,
        update = {
            set(text) { setText(it) }
            set(adaptiveHeight) { adaptive -> textStyle { it.adaptiveHeight(adaptive) } }
        },
    )
}

/**
 * @param onClick fires on mouse-down and plays the click sound, matching
 *   [com.lowdragmc.lowdraglib2.gui.ui.elements.Button]'s own behaviour. For a press-and-release
 *   click, use [UIModifier.onClick] on a [Box] instead.
 * @param text the button's own label. Leave it null and pass [content] to put arbitrary elements
 *   inside instead.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: UIModifier = UIModifier,
    text: Component? = null,
    content: @Composable () -> Unit = {},
) {
    // The listener is registered once and reads through the holder, so a fresh lambda every
    // recomposition does not mean a fresh registration every recomposition.
    val holder = remember { EventHandlerHolder() }
    holder.handler = { onClick() }
    UIElementNode(
        factory = { LdlButton().also { it.setOnClick(holder) } },
        modifier = modifier,
        update = { set(text) { if (it == null) noText() else setText(it) } },
        content = content,
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    text: String,
    modifier: UIModifier = UIModifier,
) = Button(onClick, modifier, Component.translatable(text))

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
    modifier: UIModifier,
    update: Updater<E>.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val holder = remember { ValueChangeHolder<V>() }
    holder.onValueChange = onValueChange
    UIElementNode(
        // The subscription lives exactly as long as the element does, and the element is discarded
        // wholesale when the node leaves the composition — so there is nothing to unsubscribe.
        factory = { factory().also { e -> e.registerValueListener { v -> holder.onValueChange?.invoke(v) } } },
        modifier = modifier,
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
    modifier: UIModifier = UIModifier,
) = BindableNode({ TextField() }, value, onValueChange, modifier)

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: UIModifier = UIModifier,
) = BindableNode({ Switch() }, checked, onCheckedChange, modifier)

/**
 * @param text the label beside the box. Defaults to none — the underlying widget ships with the
 *   literal caption "Toggle", which is a reasonable placeholder for an editor palette and never what
 *   a caller wants.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: UIModifier = UIModifier,
    text: Component? = null,
    content: @Composable () -> Unit = {},
) = BindableNode(
    factory = { Toggle() },
    value = checked,
    onValueChange = onCheckedChange,
    modifier = modifier,
    update = { set(text) { toggleLabel.setText(it ?: Component.empty()) } },
    content = content,
)

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: UIModifier = UIModifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
) = BindableNode(
    factory = { Slider.Horizontal() },
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    update = { set(range) { setRange(it.start, it.endInclusive) } },
)

@Composable
fun <T> Selector(
    selected: T?,
    candidates: List<T>,
    onSelectedChange: (T?) -> Unit,
    modifier: UIModifier = UIModifier,
) = BindableNode<Selector<T?>, T?>(
    // Selector<T?> rather than Selector<T>: "nothing selected" is a real state here, and the
    // element's own value type has to admit it for the controlled-value round trip to typecheck.
    factory = { Selector<T?>() },
    value = selected,
    onValueChange = onSelectedChange,
    modifier = modifier,
    update = { set(candidates) { setCandidates(it) } },
)

@Composable
fun ItemSlot(
    item: ItemStack,
    onItemChange: (ItemStack) -> Unit = {},
    modifier: UIModifier = UIModifier,
) = BindableNode({ ItemSlot() }, item, onItemChange, modifier)

@Composable
fun FluidSlot(
    fluid: FluidStack,
    onFluidChange: (FluidStack) -> Unit = {},
    modifier: UIModifier = UIModifier,
) = BindableNode({ FluidSlot() }, fluid, onFluidChange, modifier)

@Composable
fun ColorSelector(
    color: Int,
    onColorChange: (Int) -> Unit,
    modifier: UIModifier = UIModifier,
) = BindableNode({ ColorSelector() }, color, onColorChange, modifier)

@Composable
fun TagField(
    tag: Tag?,
    onTagChange: (Tag?) -> Unit,
    modifier: UIModifier = UIModifier,
) = BindableNode<TagField, Tag?>({ TagField() }, tag, onTagChange, modifier)

/** A draggable scrollbar as a value in its own right, for driving something by hand. */
@Composable
fun ScrollerHorizontal(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: UIModifier = UIModifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
) = BindableNode(
    factory = { Scroller.Horizontal() },
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    update = { set(range) { setRange(it.start, it.endInclusive) } },
)

@Composable
fun ScrollerVertical(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: UIModifier = UIModifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
) = BindableNode(
    factory = { Scroller.Vertical() },
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    update = { set(range) { setRange(it.start, it.endInclusive) } },
)

@Composable
fun SliderVertical(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: UIModifier = UIModifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
) = BindableNode(
    factory = { Slider.Vertical() },
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    update = { set(range) { setRange(it.start, it.endInclusive) } },
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
    modifier: UIModifier = UIModifier,
) = BindableNode<BindableValue<T>, T>({ BindableValue() }, value, onValueChange, modifier)

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
    modifier: UIModifier = UIModifier,
) {
    val holder = remember { ValueChangeHolder<List<String>>() }
    holder.onValueChange = onLinesChange
    UIElementNode(
        factory = {
            TextArea().also { area ->
                area.registerValueListener { v -> holder.onValueChange?.invoke(v?.toList().orEmpty()) }
            }
        },
        modifier = modifier,
        update = {
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
    modifier: UIModifier = UIModifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    text: Component? = null,
    content: @Composable () -> Unit = {},
) {
    UIElementNode(
        factory = { ProgressBar() },
        modifier = modifier,
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
    modifier: UIModifier = UIModifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    val view = remember { SplitView.Horizontal() }
    UIElementNode(factory = { view }, modifier = modifier)
    Subcompose(view.first, left)
    Subcompose(view.second, right)
}

@Composable
fun SplitViewVertical(
    modifier: UIModifier = UIModifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    val view = remember { SplitView.Vertical() }
    UIElementNode(factory = { view }, modifier = modifier)
    Subcompose(view.first, top)
    Subcompose(view.second, bottom)
}

/** [TextArea] with syntax highlighting. */
@Composable
fun CodeEditor(
    lines: List<String>,
    onLinesChange: (List<String>) -> Unit,
    language: ILanguageDefinition,
    modifier: UIModifier = UIModifier,
) {
    val holder = remember { ValueChangeHolder<List<String>>() }
    holder.onValueChange = onLinesChange
    UIElementNode(
        factory = {
            LdlCodeEditor().also { editor ->
                editor.registerValueListener { v -> holder.onValueChange?.invoke(v?.toList().orEmpty()) }
            }
        },
        modifier = modifier,
        update = {
            set(language) { setLanguage(it) }
            set(lines) { next -> if (getValue()?.toList() != next) setValue(next.toTypedArray(), false) }
        },
    )
}

/** A tree editor for NBT. */
@Composable
fun StructuredTagEditor(
    tag: Tag?,
    onTagChange: (Tag?) -> Unit,
    modifier: UIModifier = UIModifier,
) = BindableNode<StructuredTagEditor, Tag?>({ StructuredTagEditor() }, tag, onTagChange, modifier)

/** Embeds a UI saved by the editor. */
@Composable
fun UITemplate(templatePath: IResourcePath?, modifier: UIModifier = UIModifier) {
    UIElementNode(
        factory = { UITemplateElement() },
        modifier = modifier,
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
 * caller that ignores it will find `visible` and the dialog disagreeing.
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
    modifier: UIModifier = UIModifier,
    content: @Composable () -> Unit,
) {
    val parent = LocalUIElement.current
    val dialog = remember { LdlDialog() }
    val applied = remember { AppliedModifier() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // Before the show effect below, so its SideEffect runs first and the content is in place by the
    // time `show()` inspects the containers.
    Subcompose(dialog.contentContainer, content)
    SideEffect { applied.applyTo(dialog, modifier) }

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
fun InventorySlots(modifier: UIModifier = UIModifier) {
    UIElementNode(factory = { InventorySlots() }, modifier = modifier)
}

/** Groups the [Toggle]s composed inside it, so at most one is on at a time. */
@Composable
fun ToggleGroup(modifier: UIModifier = UIModifier, content: @Composable () -> Unit = {}) {
    UIElementNode(factory = { ToggleGroupElement() }, modifier = modifier, content = content)
}

// endregion
