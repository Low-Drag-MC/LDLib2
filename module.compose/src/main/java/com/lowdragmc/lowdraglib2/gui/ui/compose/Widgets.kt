package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Updater
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.LDLib2
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
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyLayoutStyleDsl
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.nbt.EndTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button as LdlButton
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog as LdlDialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor as LdlCodeEditor
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots as LdlInventorySlots

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
 *
 * @param layout the scroll view's own box: its size, margins, where it sits.
 * @param contentLayout the container the children are laid out in: `gap`, `padding`, direction.
 *   Putting those in [layout] instead spaces the scrollbars, not the rows.
 */
@Composable
fun ScrollView(
    scrollerStyle: (ScrollerView.ScrollerViewStyle.() -> Unit)? = null,
    contentLayout: LayoutBlock? = null,
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
        // Null when neither is given, so a plain ScrollView is not observed for nothing.
        widgetStyle = if (scrollerStyle == null && contentLayout == null) null else {
            {
                scrollerStyle?.let { getScrollerViewStyle().apply(it) }
                contentLayout?.let { TaffyLayoutStyleDsl(viewContainer.layout).it() }
            }
        },
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

/**
 * [Text] as LDLib2's `Label` element, for stylesheets that target `label` rather than `text`.
 *
 * <p>Displays [text] exactly like [Text] does. The element's binding support is not reachable from
 * here: a binding registers a sync value, which a composition must never do. Drive the text from
 * state instead, filled by a binding built in `createUI`.
 */
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
 * A push button.
 *
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
        update = {
            write(text) {
                if (it == null) {
                    noText()
                } else {
                    // noText() hides the label, and setText() alone does not bring it back.
                    enableText()
                    setText(it)
                }
            }
        },
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
    // Literal, like Text(String): a caption is shown as written. Pass a Component to translate.
    onClick, Component.literal(text), buttonStyle, layout, style, classes, id,
    enabled, visible, focusable, events, content,
)

// endregion

// region value widgets

/**
 * Reports a controlled widget's edits back to the composition, and makes sure every edit leads to a
 * recomposition of the widget's node.
 *
 * <p>That second half is what lets a caller *reject* an edit. Updating the element only when the
 * composed value changes would leave a refused edit sitting in the element for good: the caller kept
 * its state as it was, so nothing changed for the composition to react to. Counting edits in state,
 * and reading the count in the node's body, gives every edit its own recomposition, in which the
 * element is put back in line with whatever the caller decided.
 */
private class EditReporter<V> {
    var onValueChange: ((V) -> Unit)? = null
    var edits by mutableIntStateOf(0)
        private set

    fun report(value: V) {
        // A widget reacting to what the composition just did - re-clamping into a new range, a toggle
        // group unchecking a sibling - reports through the same listener. That is not an edit.
        if (CompositionWrites.active) return
        onValueChange?.invoke(value)
        edits++
    }
}

/** [Updater.set], with the write marked as the composition's own. See [CompositionWrites]. */
internal inline fun <E, T> Updater<E>.write(value: T, crossinline block: E.(T) -> Unit) =
    set(value) { v -> CompositionWrites.writing { block(v) } }

/**
 * The shared shape of every controlled widget here: the composition owns the value, the element
 * reports edits back through [onValueChange].
 *
 * <p>On every recomposition [configure] runs first — ranges, candidates, a language — and then the
 * element is brought back to [value] if it holds something else. That order matters: a value is only
 * valid relative to its range, and writing it into a range that is about to change would clamp it
 * against the wrong bounds. Every edit forces such a recomposition (see [EditReporter]), so an edit
 * the caller accepted settles and one it refused is undone.
 *
 * @param fromElement the composition's view of the element's value; also what is reported, so it
 *   should copy anything the widget goes on mutating in place.
 * @param toElement the reverse, for writing.
 * @param same how a held value and a composed one are compared. Most value types compare by
 *   equality; `ItemStack` does not, and would otherwise be rewritten on every recomposition.
 * @param writeValue how a value is written. Text widgets use it to keep the cursor where it was.
 */
@Composable
internal fun <E : BindableUIElement<EV>, EV, V> ControlledNode(
    factory: () -> E,
    value: V,
    onValueChange: (V) -> Unit,
    fromElement: (EV) -> V,
    toElement: (V) -> EV,
    layout: LayoutBlock?,
    style: StyleBlock?,
    classes: String?,
    id: String?,
    enabled: Boolean,
    visible: Boolean,
    focusable: Boolean?,
    events: (EventScope.() -> Unit)?,
    widgetStyle: (E.() -> Unit)? = null,
    same: (V, V) -> Boolean = { a, b -> a == b },
    writeValue: E.(EV) -> Unit = { setValue(it, false) },
    configure: Updater<E>.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val reporter = remember { EditReporter<V>() }
    reporter.onValueChange = onValueChange
    // Read here, which is what gives each edit its recomposition.
    val edits = reporter.edits
    UIElementNode(
        // The listener lives exactly as long as the element does, and the element is discarded
        // wholesale when the node leaves the composition - so there is nothing to unsubscribe.
        factory = { factory().also { e -> e.registerValueListener { v -> reporter.report(fromElement(v)) } } },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = widgetStyle,
        update = {
            // Captures the edit count, and that is load-bearing. After a refused edit nothing else
            // this lambda captures has changed, so the compiler would hand UIElementNode the same
            // instance as last time, the node would be skipped, and the reconcile below would never
            // run to undo the edit.
            set(edits) {}
            configure()
            reconcile {
                if (!same(fromElement(getValue()), value)) CompositionWrites.writing { writeValue(toElement(value)) }
            }
        },
        content = content,
    )
}

/** [ControlledNode] for widgets whose value type is already the one the caller works with. */
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
    configure: Updater<E>.() -> Unit = {},
    content: @Composable () -> Unit = {},
) = ControlledNode(
    factory, value, onValueChange, { it }, { it },
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = widgetStyle, configure = configure, content = content,
)

/** Holds the latest of a caller's lambdas for a widget that takes one once, in its factory. */
internal class Latest<T>(var value: T)

/**
 * Single-line text input.
 *
 * <p>Three ways to restrict what is entered, which behave differently:
 *
 * <ul>
 * <li>[charFilter] refuses a character before it lands: it never appears.</li>
 * <li>[validator] judges the whole text. Text it rejects stays in the field, shown as an error, and
 *     is not reported — [onValueChange] only ever sees valid text. That is LDLib2's own behaviour, and
 *     it lets the player pass through an invalid state on the way to a valid one.</li>
 * <li>Not storing a value in [onValueChange] also works: the field goes back to what the state holds
 *     on the next frame. Use it for corrections, such as clamping a number into range.</li>
 * </ul>
 *
 * @param charFilter whether a typed character may be inserted. Null accepts any.
 * @param validator whether the whole text is valid. Null accepts anything.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    charFilter: ((Char) -> Boolean)? = null,
    validator: ((String) -> Boolean)? = null,
    fieldStyle: (TextField.TextFieldStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val currentValidator = remember { Latest<((String) -> Boolean)?>(null) }
    val currentFilter = remember { Latest<((Char) -> Boolean)?>(null) }
    currentValidator.value = validator
    currentFilter.value = charFilter
    ControlledNode(
        factory = {
            TextField().apply {
                setTextValidator { text -> currentValidator.value?.invoke(text) ?: true }
                setCharValidator { char -> currentFilter.value?.invoke(char) ?: true }
            }
        },
        value = value,
        onValueChange = onValueChange,
        fromElement = { it.orEmpty() },
        toElement = { it },
        layout, style, classes, id, enabled, visible, focusable, events,
        widgetStyle = fieldStyle?.let { block -> { getTextFieldStyle().apply(block) } },
        // setValue puts the cursor at the end, which is wrong for a correction made while typing in
        // the middle.
        writeValue = { text ->
            val cursor = cursorPos
            setValue(text, false)
            setCursor(cursor)
            setSelection(cursor, cursor)
        },
    )
}

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
 *   a caller wants. With none the label is hidden, not left as empty space.
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
    configure = {
        write(text) {
            if (it == null) {
                noText()
            } else {
                enableText()
                toggleLabel.setText(it)
            }
        }
    },
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
    configure = { write(range) { setRange(it.start, it.endInclusive) } },
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
    configure = { write(range) { setRange(it.start, it.endInclusive) } },
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
    configure = { write(range) { setRange(it.start, it.endInclusive) } },
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
    configure = { write(range) { setRange(it.start, it.endInclusive) } },
)

/**
 * A drop-down choice among [candidates].
 *
 * @param candidateText how a candidate is labelled, in the drop-down and on the closed selector.
 *   Null keeps the widget's own default, `Component.translatable(candidate.toString())`. Read when
 *   the candidates are (re)built, so changing it alone does not relabel a list already shown.
 */
@Composable
fun <T> Selector(
    selected: T?,
    onSelectedChange: (T?) -> Unit,
    candidates: List<T>,
    candidateText: ((T) -> Component)? = null,
    selectorStyle: (Selector<T?>.SelectorStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val currentText = remember { Latest<((T) -> Component)?>(null) }
    currentText.value = candidateText
    BindableNode<Selector<T?>, T?>(
        // Selector<T?> rather than Selector<T>: "nothing selected" is a real state here, and the
        // element's own value type has to admit it for the controlled-value round trip to typecheck.
        factory = {
            Selector<T?>().apply {
                setCandidateUIProvider(UIElementProvider.text { candidate ->
                    val text = currentText.value
                    if (candidate != null && text != null) text(candidate)
                    else Component.translatable(candidate?.toString() ?: "---")
                })
            }
        },
        value = selected,
        onValueChange = onSelectedChange,
        layout, style, classes, id, enabled, visible, focusable, events,
        widgetStyle = selectorStyle?.let { block -> { getSelectorStyle().apply(block) } },
        configure = { write(candidates) { setCandidates(it) } },
    )
}

/**
 * A single item slot holding [item], not bound to any inventory.
 *
 * <p>In a menu, a slot that has to take part in the menu's slot sync is a different thing: build it in
 * `createUI`, on both sides, bind it there, and [Mount] it.
 */
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
) = ControlledNode(
    { ItemSlot() }, item, onItemChange,
    // Copied: the widget keeps the stack it reports, and a caller holding that instance would see it
    // change underneath it.
    fromElement = { it?.copy() ?: ItemStack.EMPTY },
    toElement = { it },
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = slotStyle?.let { block -> { getSlotStyle().apply(block) } },
    // ItemStack has no equals of its own; compared by identity every recomposition would rewrite it.
    same = { a, b -> ItemStack.matches(a, b) },
)

/**
 * A single fluid slot holding [fluid], not bound to any tank. See [ItemSlot] for menus.
 *
 * @param capacity what a full slot holds, for the fill level and the tooltip. Zero, the default,
 *   draws any non-empty fluid as full.
 */
@Composable
fun FluidSlot(
    fluid: FluidStack,
    onFluidChange: (FluidStack) -> Unit = {},
    capacity: Int = 0,
    slotStyle: (FluidSlot.SlotStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = ControlledNode(
    { FluidSlot() }, fluid, onFluidChange,
    fromElement = { it?.copy() ?: FluidStack.EMPTY },
    toElement = { it },
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = slotStyle?.let { block -> { getSlotStyle().apply(block) } },
    same = { a, b -> FluidStack.matches(a, b) },
    configure = { write(capacity) { setCapacity(it) } },
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

/**
 * A tag written as SNBT text.
 *
 * <p>Null is "no tag": what an empty field holds, and what it reports when cleared. (The widget
 * itself uses `EndTag` for that; it is not passed through.)
 */
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
) = ControlledNode<TagField, Tag, Tag?>(
    { TagField() }, tag, onTagChange,
    fromElement = { it.orNullTag()?.copy() },
    toElement = { it ?: EndTag.INSTANCE },
    layout, style, classes, id, enabled, visible, focusable, events,
)

/**
 * A headless value holder: renders nothing.
 *
 * <p>Inside a composition it is only a mirror of [value] — it can't carry a synced binding, because
 * nothing that registers a sync value may be created in a composition (see "服务端驱动的 UI" in
 * compose.md). It exists so the element is available declaratively at all; a binding placeholder
 * belongs in `createUI`, built by hand.
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

/**
 * A tag edited as a tree of rows. Null is "no tag", as for [TagField].
 *
 * <p>The widget edits its tag in place, so what it reports is a copy — otherwise the caller's state
 * would hold the very instance the next edit mutates, and a state write of an unchanged reference
 * notifies nobody.
 */
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
) = ControlledNode<StructuredTagEditor, Tag, Tag?>(
    { StructuredTagEditor() }, tag, onTagChange,
    fromElement = { it.orNullTag()?.copy() },
    toElement = { it ?: EndTag.INSTANCE },
    layout, style, classes, id, enabled, visible, focusable, events,
)

private fun Tag?.orNullTag(): Tag? = if (this == null || this is EndTag) null else this

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
) = ControlledNode(
    { TextArea() }, lines, onLinesChange,
    fromElement = { it?.toList().orEmpty() },
    toElement = { it.toTypedArray() },
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = areaStyle?.let { block -> { getTextAreaStyle().apply(block) } },
    writeValue = { keepingCursor { setValue(it, false) } },
)

/**
 * [TextArea] with syntax highlighting.
 *
 * @param areaStyle the text area's own styling, which the editor inherits.
 */
@Composable
fun CodeEditor(
    lines: List<String>,
    onLinesChange: (List<String>) -> Unit,
    language: ILanguageDefinition,
    areaStyle: (TextArea.TextAreaStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) = ControlledNode(
    { LdlCodeEditor() }, lines, onLinesChange,
    fromElement = { it?.toList().orEmpty() },
    toElement = { it.toTypedArray() },
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = areaStyle?.let { block -> { getTextAreaStyle().apply(block) } },
    writeValue = { keepingCursor { setValue(it, false) } },
    configure = { write(language) { setLanguage(it) } },
)

/**
 * Runs [write] without losing the caret. `TextArea#setValue` resets the cursor, the selection and the
 * scroll to the top left, which is right for loading a document and wrong for a correction applied
 * while the user is typing — they would be thrown back to the first line on every one.
 */
private inline fun <A : TextArea> A.keepingCursor(write: A.() -> Unit) {
    val cursor = cursorPos()
    write()
    setCursor(cursor.line(), cursor.col())
    collapseSelectionToCursor()
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
) {
    UIElementNode(
        factory = { ProgressBar() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = barStyle?.let { block -> { getProgressBarStyle().apply(block) } },
        update = {
            // Range before progress, so the progress is clamped against the bounds it belongs to.
            write(range) { setRange(it.start, it.endInclusive) }
            write(progress) { setProgress(it) }
            write(text) { label.setText(it ?: Component.empty()) }
        },
    )
}

/**
 * Two panes with a draggable divider between them.
 *
 * <p>Each pane is its own [Subcompose], because the widget has two child lists and an applier only
 * knows one. Everything else behaves normally — state, `CompositionLocal`s and recomposition all
 * cross the pane boundary as if it were not there.
 *
 * @param initialPercentage where the divider starts, as a percentage of the width. Only the start:
 *   the player drags it from there, and the widget reports no drag to follow.
 */
@Composable
fun SplitViewHorizontal(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    initialPercentage: Float = 50f,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val view = remember { SplitView.Horizontal().apply { setPercentage(initialPercentage) } }
    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    )
    Subcompose(view.first, left)
    Subcompose(view.second, right)
}

/** [SplitViewHorizontal], stacked. [initialPercentage] is of the height. */
@Composable
fun SplitViewVertical(
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    initialPercentage: Float = 50f,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val view = remember { SplitView.Vertical().apply { setPercentage(initialPercentage) } }
    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    )
    Subcompose(view.first, top)
    Subcompose(view.second, bottom)
}

/**
 * Embeds a UI saved by the editor.
 *
 * <p>A different [templatePath] builds a new element rather than reloading the old one: loading a
 * template resets the element's own layout and style to the template's, which would quietly demote
 * whatever [layout] and [style] had already written.
 */
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
    key(templatePath) {
        UIElementNode(
            factory = { UITemplateElement().apply { setTemplate(templatePath) } },
            layout = layout, style = style, classes = classes, id = id,
            enabled = enabled, visible = visible, focusable = focusable, events = events,
        )
    }
}

/**
 * A modal dialog, driven by [visible] rather than by calling `show()`.
 *
 * ```kotlin
 * var confirming by remember { mutableStateOf(false) }
 * Button(onClick = { confirming = true }, text = "Delete")
 * Dialog(
 *     visible = confirming,
 *     onDismiss = { confirming = false },
 *     title = { Text("Delete it?") },
 *     buttons = { Button(onClick = { delete(); confirming = false }, text = "Delete") },
 * ) {
 *     Text("This cannot be undone.")
 * }
 * ```
 *
 * <p>Shown over the whole UI, not just over the element this composition lives in. Its slots are
 * composed only while it is showing, so effects inside them stop when it closes.
 *
 * <p>The player can close it — Escape, and a click outside if [dismissOnClickOutside] — and
 * [onDismiss] reports that; a caller that ignores it will find [visible] and the dialog disagreeing.
 * It is *not* called when the dialog closes because [visible] went false or the dialog left the
 * composition: that was the caller's own doing.
 *
 * @param layout the dialog window's layout, e.g. its width. The default window is 150px wide.
 * @param style the dialog window's appearance.
 */
@Composable
fun Dialog(
    visible: Boolean,
    onDismiss: () -> Unit = {},
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable () -> Unit)? = null,
    dismissOnEscape: Boolean = true,
    dismissOnClickOutside: Boolean = false,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    content: @Composable () -> Unit,
) {
    val host = LocalUIElement.current
    val observer = LocalNodeObserver.current
    val dialog = remember { LdlDialog() }
    val applied = remember { AppliedProps<UIElement>() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // Before the show effect below. The slots fill their containers from DisposableEffects, which the
    // runtime runs in order, so they are populated by the time `show()` decides which of the title,
    // content and button rows to display - it hides the ones that are empty.
    if (visible) {
        title?.let { Subcompose(dialog.titleBar, it) }
        Subcompose(dialog.contentContainer, content)
        buttons?.let { Subcompose(dialog.buttonContainer, it) }
    }
    SideEffect {
        dialog.setAutoClose(dismissOnEscape)
        dialog.setClickOutsideClose(dismissOnClickOutside)
        applied.apply(
            observer, dialog.overlay, layout, style, widget = null,
            classes = classes, id = id, enabled = true, visible = true, focusable = null, events = null,
        )
    }
    DisposableEffect(dialog) {
        dialog.setOnClose { if (!CompositionWrites.active) currentOnDismiss() }
        onDispose { dialog.setOnClose(null) }
    }
    DisposableEffect(visible, host) {
        if (visible) CompositionWrites.writing { dialog.show(host.modularUI?.ui?.rootElement ?: host) }
        onDispose { if (visible) CompositionWrites.writing { dialog.close() } }
    }
}

/**
 * The player's inventory and hotbar — for a screen that is not a menu.
 *
 * <p>In a menu the slots have to be registered with the menu, on both sides, in the same order; a
 * composition only runs on the client, so it cannot do that. Build `InventorySlots()` in `createUI`
 * and [Mount] it instead. Used inside a menu, this one logs an error and stays unbound rather than
 * registering client-only slots the server would not know about.
 */
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
        factory = { ComposedInventorySlots() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    )
}

/** [LdlInventorySlots] that refuses to bind menu slots from inside a composition. */
private class ComposedInventorySlots : LdlInventorySlots() {
    override fun onModularUIChanged(event: UIEvent) {
        if (modularUI?.menu != null) {
            LDLib2.LOGGER.error(
                "InventorySlots composed inside a menu UI: its slots would exist on the client only. " +
                    "Build InventorySlots in createUI and Mount it instead.",
            )
        }
    }
}

/**
 * Groups the [Toggle]s placed *directly* inside it, so that at most one is on.
 *
 * <p>Direct children only — a toggle inside a `Row` inside this is not part of the group. Programmatic
 * changes the group makes as a result (unchecking the previous toggle, or checking another one when
 * the checked toggle is removed) are not reported to those toggles' `onCheckedChange`: drive the
 * toggles from one piece of state, as in `checked = selected == "a"`, and the group just keeps the
 * display consistent.
 *
 * @param allowEmpty whether unchecking the checked toggle leaves none checked. Off by default, matching
 *   the widget: one is always on.
 */
@Composable
fun ToggleGroup(
    allowEmpty: Boolean = false,
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
        update = { write(allowEmpty) { toggleGroup.setAllowEmpty(it) } },
        content = content,
    )
}

// endregion
