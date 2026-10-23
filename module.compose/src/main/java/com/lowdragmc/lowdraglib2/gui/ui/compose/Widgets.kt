package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Updater
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.lowdragmc.lowdraglib2.LDLib2
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent
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
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyLayoutStyleDsl
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import net.minecraft.nbt.EndTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import java.util.function.LongConsumer
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
 * A scrolling container. Children go into its view container, so they scroll.
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
        // Null when neither is given, so a plain ScrollView is not observed.
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
 * Turns a text element's `adaptive-height` off while the caller's `layout` block gives it a height and
 * on otherwise, writing only when that flips. Must run after the caller's block, the only point where
 * a caller height is known.
 */
private class AdaptiveGate {
    private var last: Boolean? = null

    fun update(element: TextElement, callerLayout: Boolean) {
        val adaptive = !callerLayout || !element.callerSet(LayoutProperties.HEIGHT)
        if (last == adaptive) return
        last = adaptive
        element.getTextStyle().asDefault { adaptiveHeight(adaptive) }
    }
}

/** The widget block a text wrapper hands its node; null when there is nothing to observe. */
private fun <E : TextElement> adaptiveTextBlock(
    gate: AdaptiveGate,
    textStyle: (TextElement.TextStyle.() -> Unit)?,
    layout: LayoutBlock?,
): (E.() -> Unit)? = if (textStyle == null && layout == null) null else {
    {
        textStyle?.let { getTextStyle().apply(it) }
        gate.update(this, layout != null)
    }
}

/**
 * Text sized to its content (`adaptive-height` on), unless the caller's own `layout` block gives it a
 * height, including one it only gives depending on state it reads.
 *
 * <p>A height from a stylesheet does not count: adaptive height outranks it, so a rule that sizes
 * text has to say `adaptive-height: false` too.
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
        widgetStyle = adaptiveTextBlock(gate, textStyle, layout),
        update = {
            set(text) { setText(it) }
            if (layout == null) reconcile { gate.update(this, callerLayout = false) }
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
 * <p>The element's binding support is not exposed: a binding registers a sync value, which a
 * composition must never do. Drive the text from state filled by a binding built in `createUI`.
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
        widgetStyle = adaptiveTextBlock(gate, textStyle, layout),
        update = {
            set(text) { setText(it) }
            if (layout == null) reconcile { gate.update(this, callerLayout = false) }
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
    // Literal, like Text(String); pass a Component to translate.
    onClick, Component.literal(text), buttonStyle, layout, style, classes, id,
    enabled, visible, focusable, events, content,
)

// endregion

// region value widgets

/**
 * Reports a controlled widget's edits and counts each in state, so every edit recomposes the node and a
 * refused one is undone. Changes made under [CompositionWrites] are not reported, but still get one
 * recomposition if they leave the element disagreeing with the composed value.
 */
private class EditReporter<V> {
    var onValueChange: ((V) -> Unit)? = null
    var edits by mutableIntStateOf(0)
        private set

    private var hasComposed = false
    private var composed: V? = null
    private var same: (V, V) -> Boolean = { a, b -> a == b }
    // At most one correction per composed value, or two contradictory siblings (two toggles checked in
    // a one-of group) correct each other every frame.
    private var corrected = false

    /** The value the composition is about to bring the element in line with. Apply phase only. */
    fun settle(value: V, same: (V, V) -> Boolean) {
        @Suppress("UNCHECKED_CAST")
        if (!hasComposed || !same(composed as V, value)) corrected = false
        hasComposed = true
        composed = value
        this.same = same
    }

    fun report(value: V) {
        if (CompositionWrites.active) {
            @Suppress("UNCHECKED_CAST")
            if (hasComposed && !corrected && !same(value, composed as V)) {
                corrected = true
                edits++
            }
            return
        }
        corrected = false
        onValueChange?.invoke(value)
        edits++
    }
}

/** [Updater.set], with the write marked as the composition's own. See [CompositionWrites]. */
internal inline fun <E, T> Updater<E>.write(value: T, crossinline block: E.(T) -> Unit) =
    set(value) { v -> CompositionWrites.writing { block(v) } }

/**
 * The shared shape of every controlled widget: the composition owns [value], the element reports edits
 * through [onValueChange]. Each recomposition runs [configure] (ranges, candidates) before bringing the
 * element back to [value], so the value is clamped against the new bounds.
 *
 * @param fromElement also what is reported, so it should copy anything the widget mutates in place.
 * @param same compares a held value with a composed one; `ItemStack` has no value equality.
 * @param writeValue how a value is written; text widgets use it to keep the cursor.
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
        // No unsubscribe: the listener is discarded with the element when the node leaves.
        factory = { factory().also { e -> e.registerValueListener { v -> reporter.report(fromElement(v)) } } },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = widgetStyle,
        update = {
            // Load-bearing capture: after a refused edit nothing else captured changes, so without it
            // the node is skipped and the reconcile below never undoes the edit.
            set(edits) {}
            // Before configure: a range written there re-clamps, and that has to be judged against
            // this value, not the previous one.
            reconcile { reporter.settle(value, same) }
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
 * The element name [type] is registered under. A wrapper's subclass must report it as its own, or it
 * loses its theme: stylesheets select by that name (`text-field { }`) and the annotation is not inherited.
 */
internal fun registeredName(type: Class<out UIElement>): String = type.getAnnotation(LDLRegister::class.java).name

/**
 * Single-line text input.
 *
 * <p>Three ways to restrict what is entered, which behave differently:
 *
 * <ul>
 * <li>[charFilter] refuses a typed character before it lands. Text that contains a refused character
 *     anyway — pasted, say — is treated like text [validator] rejects.</li>
 * <li>[validator] judges the whole text. Text it rejects stays in the field, shown as an error, and
 *     is not reported — [onValueChange] only ever sees valid text.</li>
 * <li>Not storing a value in [onValueChange] also works: the field goes back to what the state holds
 *     on the next frame. Use it for corrections, such as clamping a number into range.</li>
 * </ul>
 *
 * <p>The error display follows the text currently in the field, including [value] when it is written
 * in and a [validator] or [charFilter] that changes. Text the player typed while it was invalid is
 * reported as soon as a changed validator accepts it.
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
            ComposedTextField().apply {
                accepts = { text ->
                    val filter = currentFilter.value
                    (filter == null || text.all(filter)) && (currentValidator.value?.invoke(text) ?: true)
                }
                setCharValidator { char -> currentFilter.value?.invoke(char) ?: true }
            }
        },
        value = value,
        onValueChange = onValueChange,
        fromElement = { it.orEmpty() },
        toElement = { it },
        layout, style, classes, id, enabled, visible, focusable, events,
        widgetStyle = fieldStyle?.let { block -> { getTextFieldStyle().apply(block) } },
        // setValue moves the cursor to the end, wrong for a correction made while typing mid-text.
        writeValue = { text ->
            val cursor = cursorPos
            setValue(text, false)
            setCursor(cursor)
            setSelection(cursor, cursor)
            revalidate()
        },
        // Not inside CompositionWrites: typed text a changed validator now accepts is reported.
        configure = { reconcile { revalidate() } },
    )
}

/** [TextField] whose error state can be brought up to date after a write the widget didn't validate. */
private class ComposedTextField : TextField() {
    var accepts: (String) -> Boolean = { true }

    override fun name(): String = registeredName(TextField::class.java)

    init {
        setTextValidator { accepts(it) }
    }

    /** `setValue` and a new validator both leave `isError` as the last keystroke computed it. */
    fun revalidate() {
        if (isError == accepts(rawText)) onRawTextUpdate()
    }
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
 * @param text the label beside the box. Null, the default, hides the label rather than leaving empty
 *   space or the widget's placeholder caption "Toggle".
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
    factory = { ComposedToggle() },
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

/** [Toggle] whose uncheck by its group, from inside a sibling's change, is not reported as an edit. */
private class ComposedToggle : Toggle() {
    override fun name(): String = registeredName(Toggle::class.java)

    override fun setValue(value: Boolean?, notify: Boolean): Toggle {
        val group = toggleGroup
        if (notify && value == false && isOn && group != null && group.toggles.any { it !== this && it.isOn }) {
            return CompositionWrites.writing { super.setValue(value, true) }
        }
        return super.setValue(value, notify)
    }
}

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
 * @param candidates what can be picked. Compared by content, so a list changed in place — a
 *   `mutableStateListOf` — is picked up as well as a new one.
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
    // Selector<T?>: "nothing selected" is a real state, which the controlled round trip must admit.
    val selector = remember {
        Selector<T?>().apply {
            setCandidateUIProvider(UIElementProvider.text { candidate ->
                val text = currentText.value
                if (candidate != null && text != null) text(candidate)
                else Component.translatable(candidate?.toString() ?: "---")
            })
        }
    }
    PopupOfDialog(selector.dialog)
    // Copied in the body: the read makes an in-place change recompose this, the copy makes it unequal.
    val shown = candidates.toList()
    BindableNode<Selector<T?>, T?>(
        factory = { selector },
        value = selected,
        onValueChange = onSelectedChange,
        layout, style, classes, id, enabled, visible, focusable, events,
        widgetStyle = selectorStyle?.let { block -> { getSelectorStyle().apply(block) } },
        configure = { write(shown) { setCandidates(it) } },
    )
}

/**
 * A single item slot holding [item], not bound to any inventory.
 *
 * <p>A slot that takes part in a menu's slot sync must be built, bound and added to the tree in
 * `createUI`, outside the [ComposeElement]: the composition only runs on the client, so a slot it
 * places — [Mount] included — never reaches the server's menu.
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
    // Copied: the widget keeps mutating the stack it reports.
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
 * <p>Clicking it with a fluid container does nothing: that is a server action on a bound tank, and the
 * widget's RPC for it is not registered here, where it would exist on the client only.
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
    { ComposedFluidSlot() }, fluid, onFluidChange,
    fromElement = { it?.copy() ?: FluidStack.EMPTY },
    toElement = { it },
    layout, style, classes, id, enabled, visible, focusable, events,
    widgetStyle = slotStyle?.let { block -> { getSlotStyle().apply(block) } },
    same = { a, b -> FluidStack.matches(a, b) },
    configure = { write(capacity) { setCapacity(it) } },
)

/**
 * [FluidSlot] that registers no RPC: a composed element exists on the client only, so its click-to-fill
 * RPC would shift every later RPC out of step with the server's.
 */
private class ComposedFluidSlot : FluidSlot() {
    override fun name(): String = registeredName(FluidSlot::class.java)

    override fun addRPCEvent(event: RPCEvent): RPCEmitter = RPCEmitter(event) { null }
}

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
 * A headless value holder: renders nothing and only mirrors [value]. It can't carry a synced binding,
 * since nothing that registers a sync value may be created in a composition (see "服务端驱动的 UI" in
 * compose.md); a binding placeholder belongs in `createUI`, built by hand.
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
 * <p>Reports a copy, since the widget edits its tag in place.
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
 * <p>Takes a `List<String>`, not the widget's `String[]`: arrays compare by identity, so every
 * recomposition would overwrite what the user is typing.
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
 * Runs [write] without losing the caret: `TextArea#setValue` resets cursor, selection and scroll to the
 * top left, wrong for a correction made while the user is typing.
 */
private inline fun <A : TextArea> A.keepingCursor(write: A.() -> Unit) {
    val cursor = cursorPos()
    write()
    setCursor(cursor.line(), cursor.col())
    collapseSelectionToCursor()
}

/**
 * A display-only progress indicator, so there is no `onValueChange`.
 *
 * @param text the caption drawn over the bar. Null, the default, draws none rather than the bare
 *   widget's placeholder text.
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
            // Progress rewritten after every range: setRange re-clamps the stored value, and an
            // unchanged progress would never be written back once the range widens.
            write(range) {
                setRange(it.start, it.endInclusive)
                setProgress(progress)
            }
            write(progress) { setProgress(it) }
            write(text) { label.setText(it ?: Component.empty()) }
        },
    )
}

/**
 * Two panes with a draggable divider between them.
 *
 * <p>State, `CompositionLocal`s and recomposition reach into the panes as usual.
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
 * <p>A different [templatePath] builds a new element rather than reloading the old one, since loading
 * resets the element's layout and style to the template's, over what [layout] and [style] wrote.
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
 * <p>Shown over the whole UI, once this composition's element is part of one: a dialog visible from
 * the first composition opens on the first frame after. Its slots are composed only while it shows,
 * so effects inside them stop when it closes. A slot that gains or loses all its content while showing
 * gains or loses its row with it.
 *
 * <p>The player can close it: Escape if [dismissOnEscape] - in a UI built with `shouldCloseOnEsc(false)`,
 * since otherwise Escape closes the whole screen before the dialog sees it; a click outside the window if
 * [dismissOnClickOutside]; and, with either of those on, focus moving to something that is not part
 * of the dialog. Popups of [Selector] and [SearchComponent] inside it count as part of it. Each of
 * these calls [onDismiss]; a caller that ignores it will find [visible] and the dialog disagreeing.
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
    val dialog = remember { ComposedDialog() }
    val applied = remember { AppliedProps<UIElement>() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var attached by remember { mutableStateOf(host.modularUI != null) }
    if (!attached) {
        DisposableEffect(host) {
            val listener = UIEventListener { if (host.modularUI != null) attached = true }
            host.addEventListener(UIEvents.MUI_CHANGED, listener)
            onDispose { host.removeEventListener(UIEvents.MUI_CHANGED, listener) }
        }
    }

    // Before the show effect: slots fill their containers from DisposableEffects, which run in order,
    // so they are populated when `show()` hides the empty rows.
    if (visible) {
        title?.let { DialogSlot(dialog, dialog.titleBar, it) }
        DialogSlot(dialog, dialog.contentContainer, content)
        buttons?.let { DialogSlot(dialog, dialog.buttonContainer, it) }
    }
    SideEffect {
        dialog.closesOnEscape = dismissOnEscape
        dialog.setAutoClose(dismissOnEscape || dismissOnClickOutside)
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
    DisposableEffect(visible, host, attached) {
        // Shown at the root, and only once there is one: show() also takes the focus, which needs a UI.
        val mui = host.modularUI
        if (!visible || mui == null) return@DisposableEffect onDispose { }
        CompositionWrites.writing { dialog.show(mui.ui.rootElement) }
        val syncRows = LongConsumer { dialog.syncRows() }
        mui.addFrameCallback(syncRows)
        onDispose {
            mui.removeFrameCallback(syncRows)
            CompositionWrites.writing { dialog.close() }
        }
    }
}

/** The dialog the composition around it is filling, for popups that must count as part of it. */
internal val LocalDialog = staticCompositionLocalOf<LdlDialog?> { null }

@Composable
private fun DialogSlot(dialog: LdlDialog, container: UIElement, content: @Composable () -> Unit) {
    Subcompose(container) { CompositionLocalProvider(LocalDialog provides dialog, content) }
}

/**
 * Registers a popup that a widget anchors to the root as part of the dialog it sits in, if any, so
 * picking from it does not dismiss the dialog as focus leaving it.
 */
@Composable
internal fun PopupOfDialog(popup: UIElement) {
    val dialog = LocalDialog.current ?: return
    DisposableEffect(dialog, popup) {
        dialog.addExternalElement(popup)
        onDispose { dialog.removeExternalElement(popup) }
    }
}

private class ComposedDialog : LdlDialog() {
    var closesOnEscape = true

    // autoClose gates Escape, a click outside and focus loss together; Escape is split off here.
    override fun keyDown(event: UIEvent) {
        if (closesOnEscape) super.keyDown(event)
    }

    /** show() hides the rows that are empty at the time; slot content can come and go after that. */
    fun syncRows() {
        for (row in arrayOf(titleBar, contentContainer, buttonContainer)) row.setDisplay(row.children.isNotEmpty())
    }
}

/**
 * The player's inventory and hotbar, display-only, for a screen that is not a menu. The slots follow
 * what the player carries but take no clicks.
 *
 * <p>In a menu, build `InventorySlots()` in `createUI` and add it to the tree there, outside the
 * [ComposeElement] and not with [Mount]: menu slots must be registered on both sides, and a composition
 * only runs on the client. Used inside a menu, this one logs an error and stays unbound.
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

/**
 * [LdlInventorySlots] that refuses to bind menu slots from a composition, and binds the player's
 * inventory for display where there is no menu; such slots join no menu, so clicks move nothing.
 */
private class ComposedInventorySlots : LdlInventorySlots() {
    override fun name(): String = registeredName(LdlInventorySlots::class.java)

    override fun onModularUIChanged(event: UIEvent) {
        val mui = modularUI ?: return
        if (mui.menu != null) {
            LDLib2.LOGGER.error(
                "InventorySlots composed inside a menu UI: its slots would exist on the client only. " +
                    "Build InventorySlots in createUI and add it to the tree there, outside the ComposeElement.",
            )
            return
        }
        if (event.customData === mui) return
        val inventory = mui.player?.inventory ?: return
        rows.forEachIndexed { r, row ->
            row.slots.forEachIndexed { c, slot -> slot.bind(Slot(inventory, r * 9 + c + 9, 0, 0)) }
        }
        hotbar.slots.forEachIndexed { i, slot -> slot.bind(Slot(inventory, i, 0, 0)) }
    }
}

/**
 * Groups the [Toggle]s placed *directly* inside it, so that at most one is on.
 *
 * <p>Direct children only — a toggle inside a `Row` inside this is not part of the group. Drive the
 * toggles from one piece of state, as in `checked = selected == "a"`. A click reports only to the
 * toggle the player clicked: the group unchecking the previous one is not reported to it, and if the
 * click is refused the previous one is checked again. A toggle the group checks by itself — because
 * the checked one was removed — is put back to what its `checked` says, so the display never shows a
 * selection the state does not hold.
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
