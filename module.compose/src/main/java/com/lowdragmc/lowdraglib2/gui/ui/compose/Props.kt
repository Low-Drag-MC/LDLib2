package com.lowdragmc.lowdraglib2.gui.ui.compose

import com.lowdragmc.lowdraglib2.gui.ui.Style
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyLayoutStyleDsl
import com.lowdragmc.lowdraglib2.gui.ui.style.BasicStyle
import com.lowdragmc.lowdraglib2.gui.ui.style.Property
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin

/**
 * The block types every composable here accepts, named so signatures stay readable.
 *
 * <p>They are LDLib2's own style facades, not a parallel vocabulary: `layout = { width(60.px) }` is
 * the receiver [com.lowdragmc.lowdraglib2.gui.ui.layoutDsl] uses and `style = { backgroundTexture(t) }`
 * is the one `styleDsl` uses. Anything the builder DSL can express, a composable can.
 */
typealias LayoutBlock = TaffyLayoutStyleDsl.() -> Unit
typealias StyleBlock = BasicStyle.() -> Unit

/**
 * Collects a node's event handlers.
 *
 * ```kotlin
 * Box(events = { onMouseEnter { hovered = true }; onMouseLeave { hovered = false } })
 * ```
 *
 * <p>Handlers are keyed on `(type, capture)`, so re-declaring the same event replaces it rather than
 * stacking a second listener.
 */
class EventScope internal constructor() {
    internal val handlers = LinkedHashMap<EventKey, (UIEvent) -> Unit>()

    internal fun reset() = handlers.clear()

    /** Any event type, including ones with no named helper below. */
    fun on(type: String, capture: Boolean = false, handler: (UIEvent) -> Unit) {
        handlers[EventKey(type, capture)] = handler
    }

    /** Press and release on the same element. Distinct from `Button`'s own mouse-down `onClick`. */
    fun onClick(handler: (UIEvent) -> Unit) = on(UIEvents.CLICK, handler = handler)
    fun onMouseDown(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_DOWN, handler = handler)
    fun onMouseUp(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_UP, handler = handler)
    fun onMouseEnter(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_ENTER, handler = handler)
    fun onMouseLeave(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_LEAVE, handler = handler)
    fun onMouseWheel(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_WHEEL, handler = handler)
    fun onKeyDown(handler: (UIEvent) -> Unit) = on(UIEvents.KEY_DOWN, handler = handler)
    fun onCharTyped(handler: (UIEvent) -> Unit) = on(UIEvents.CHAR_TYPED, handler = handler)
    fun onFocusIn(handler: (UIEvent) -> Unit) = on(UIEvents.FOCUS_IN, handler = handler)
    fun onFocusOut(handler: (UIEvent) -> Unit) = on(UIEvents.FOCUS_OUT, handler = handler)
}

internal data class EventKey(val type: String, val capture: Boolean)

/**
 * A registered listener whose body can be swapped without re-registering.
 *
 * <p>Event lambdas capture composition state, so they are a fresh instance every recomposition.
 * Registering them directly would mean an `addEventListener`/`removeEventListener` pair per frame —
 * and since listeners are prepended, that would also quietly reshuffle dispatch order. So the
 * *holder* is registered once and recomposition only swaps what it points at.
 */
internal class EventHandlerHolder : UIEventListener {
    @JvmField
    var handler: ((UIEvent) -> Unit)? = null

    override fun handleEvent(event: UIEvent) {
        handler?.invoke(event)
    }
}

/**
 * Applies a node's props, and remembers the parts that have to be diffed rather than rewritten.
 *
 * <p>Three different disciplines are at work here, and the difference is worth stating because it is
 * the whole reason this class is small:
 *
 * <ul>
 * <li><b>Style blocks are written through, never undone.</b> That is LDLib2's own semantics —
 *     `element.styleDsl { }` has never removed anything either — and re-running them is close to free
 *     because {@code StyleBag.replaceOrPutCandidate} returns early when the value is unchanged. The
 *     consequence to know is that a property written under an `if` stays written when the condition
 *     flips; the idiomatic answer to conditional appearance is [classes] plus a stylesheet rule.</li>
 * <li><b>Flags are values, so they need no undo.</b> `enabled`/`visible` are always passed, so
 *     "false last frame, absent this frame" cannot happen. They are still compared before writing,
 *     because `setActive` toggles a style class and that is not free.</li>
 * <li><b>Classes and events are diffed</b>, because those genuinely can disappear and leaving them
 *     behind would be wrong.</li>
 * </ul>
 */
internal class AppliedProps {
    private var classes: List<String> = emptyList()
    private var id: String? = null
    private var enabled: Boolean = true
    private var visible: Boolean = true
    private var focusable: Boolean? = null
    private val listeners = HashMap<EventKey, EventHandlerHolder>()
    private val scope = EventScope()

    fun apply(
        element: UIElement,
        layout: LayoutBlock?,
        style: StyleBlock?,
        classes: String?,
        id: String?,
        enabled: Boolean,
        visible: Boolean,
        focusable: Boolean?,
        events: (EventScope.() -> Unit)?,
    ) {
        if (layout != null) TaffyLayoutStyleDsl(element.layout).layout()
        if (style != null) element.style.style()
        applyClasses(element, classes)
        applyFlags(element, id, enabled, visible, focusable)
        applyEvents(element, events)
    }

    private fun applyClasses(element: UIElement, spec: String?) {
        val next = parseClasses(spec)
        if (next == classes) return
        val (removed, added) = diffKeys(classes, next)
        for (name in removed) element.removeClass(name)
        for (name in added) element.addClasses(name)
        classes = next
    }

    private fun applyFlags(
        element: UIElement,
        id: String?,
        enabled: Boolean,
        visible: Boolean,
        focusable: Boolean?,
    ) {
        if (id != this.id) {
            element.id = id.orEmpty()
            this.id = id
        }
        if (enabled != this.enabled) {
            element.setActive(enabled)
            this.enabled = enabled
        }
        if (visible != this.visible) {
            element.setVisible(visible)
            this.visible = visible
        }
        if (focusable != null && focusable != this.focusable) {
            element.setFocusable(focusable)
            this.focusable = focusable
        }
    }

    private fun applyEvents(element: UIElement, events: (EventScope.() -> Unit)?) {
        if (events == null && listeners.isEmpty()) return
        scope.reset()
        events?.invoke(scope)
        val next = scope.handlers

        val (gone, _) = diffKeys(listeners.keys.toList(), next.keys.toList())
        for (key in gone) {
            listeners.remove(key)?.let { element.removeEventListener(key.type, it, key.capture) }
        }
        for ((key, handler) in next) {
            val holder = listeners.getOrPut(key) {
                EventHandlerHolder().also { element.addEventListener(key.type, it, key.capture) }
            }
            holder.handler = handler
        }
    }
}

/**
 * Splits `"card primary"` into the names to hand to `addClasses`.
 *
 * <p>Blank-tolerant on purpose: the idiomatic way to make a class conditional is string concatenation
 * at the call site, and `"card" + if (hot) " hot" else ""` produces a trailing space often enough.
 */
internal fun parseClasses(spec: String?): List<String> =
    spec?.split(' ')?.filter { it.isNotEmpty() }.orEmpty()

/**
 * Splits a change to a set of keys into what has to come off and what has to go on.
 *
 * <p>Used for both classes and event registrations, and separate from [AppliedProps] so it can be
 * tested: everything else in this file names [UIElement], which puts it out of reach of `src/test`.
 *
 * <p>Keys present in both lists are deliberately left alone rather than removed and re-added. For
 * events that is load-bearing — it keeps a registered [EventHandlerHolder] registered across
 * recompositions instead of churning listeners, and LDLib2 prepends listeners, so churn would also
 * quietly reverse dispatch order.
 *
 * @return the keys to remove, then the keys to add, in that order
 */
internal fun <T> diffKeys(current: List<T>, next: List<T>): Pair<List<T>, List<T>> =
    current.filter { it !in next } to next.filter { it !in current }

/**
 * Runs [block] with this facade's writes redirected to `DEFAULT` origin.
 *
 * <p>For the values a wrapper chooses on the caller's behalf — a `Row`'s flex direction, a `Text`'s
 * adaptive height. Without this they would land at `INLINE`, the same cascade slot a caller's own
 * `style = { }` writes to, and the two would clobber each other in whichever order they ran. At
 * `DEFAULT` the caller always outranks the wrapper, and so does a stylesheet.
 */
internal inline fun <S : Style> S.asDefault(block: S.() -> Unit) {
    val previous = pipelineState
    pipelineState = StyleOrigin.DEFAULT
    try {
        block()
    } finally {
        pipelineState = previous
    }
}

/** [asDefault] for the layout facade, with the same DSL receiver a `layout = { }` block gets. */
internal fun UIElement.defaultLayout(block: LayoutBlock) {
    layout.asDefault { TaffyLayoutStyleDsl(this).block() }
}

/** Whether the caller's own `layout` / `style` block set [property]. */
internal fun UIElement.callerSet(property: Property<*>): Boolean =
    styleBag.containsCandidate(property) { it.origin() == StyleOrigin.INLINE }
