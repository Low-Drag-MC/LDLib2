package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.RememberObserver
import com.lowdragmc.lowdraglib2.LDLib2
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
 * The block types every composable here accepts. They are LDLib2's own style facades: `layout = { }`
 * has the receiver [com.lowdragmc.lowdraglib2.gui.ui.layoutDsl] uses and `style = { }` the one
 * `styleDsl` uses.
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
 * <p>Each `(type, capture)` pair is registered on the element once, however many handlers are
 * declared for it and however often the block re-runs; declaring the same event twice runs both.
 */
class EventScope internal constructor() {
    internal val handlers = LinkedHashMap<EventKey, MutableList<(UIEvent) -> Unit>>()

    internal fun reset() = handlers.clear()

    /**
     * Any event type, including ones with no named helper below.
     *
     * <p>Declaring the same event twice adds a second handler rather than replacing the first.
     * Handlers do not accumulate across recompositions — the block is re-run from empty each time.
     */
    fun on(type: String, capture: Boolean = false, handler: (UIEvent) -> Unit) {
        handlers.getOrPut(EventKey(type, capture)) { ArrayList(1) }.add(handler)
    }

    /** Press and release on the same element. Distinct from `Button`'s own mouse-down `onClick`. */
    fun onClick(handler: (UIEvent) -> Unit) = on(UIEvents.CLICK, handler = handler)
    fun onMouseDown(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_DOWN, handler = handler)
    fun onMouseUp(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_UP, handler = handler)
    /**
     * Capture phase, always: `ModularUI` dispatches enter and leave with no bubble phase, targeting
     * the innermost element under the pointer, so a container also hears its children being hovered.
     */
    fun onMouseEnter(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_ENTER, capture = true, handler = handler)
    fun onMouseLeave(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_LEAVE, capture = true, handler = handler)
    fun onMouseWheel(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_WHEEL, handler = handler)
    fun onKeyDown(handler: (UIEvent) -> Unit) = on(UIEvents.KEY_DOWN, handler = handler)
    fun onCharTyped(handler: (UIEvent) -> Unit) = on(UIEvents.CHAR_TYPED, handler = handler)
    fun onFocusIn(handler: (UIEvent) -> Unit) = on(UIEvents.FOCUS_IN, handler = handler)
    fun onFocusOut(handler: (UIEvent) -> Unit) = on(UIEvents.FOCUS_OUT, handler = handler)
}

internal data class EventKey(val type: String, val capture: Boolean)

/**
 * Registered once; recomposition only swaps [handler]. Re-registering the fresh lambdas would churn
 * listeners and, since they are prepended, reshuffle dispatch order.
 */
internal class EventHandlerHolder : UIEventListener {
    @JvmField
    var handler: ((UIEvent) -> Unit)? = null

    override fun handleEvent(event: UIEvent) {
        handler?.invoke(event)
    }
}

/**
 * Applies a node's props. Style blocks are written through and never undone, like `styleDsl { }`, so
 * a property written under an `if` stays written when it flips; classes and events are diffed.
 * Flags are compared before writing because `setActive` toggles a style class.
 */
internal class AppliedProps<T : UIElement> : RememberObserver, Reappliable {
    private var classes: List<String> = emptyList()
    private var lastClassSpec: String? = null
    private var id: String? = null

    // Null until first written: a factory may hand over an element that starts disabled or hidden.
    private var enabled: Boolean? = null
    private var visible: Boolean? = null
    private var focusable: Boolean? = null
    private val listeners = HashMap<EventKey, EventHandlerHolder>()
    private val scope = EventScope()

    // The last-applied arguments, so [reapply] can run without a composition.
    private var observer: NodeObserver? = null
    private var element: T? = null
    private var layout: LayoutBlock? = null
    private var style: StyleBlock? = null
    private var widget: (T.() -> Unit)? = null
    private var classSpec: String? = null
    private var idSpec: String? = null
    private var enabledSpec: Boolean = true
    private var visibleSpec: Boolean = true
    private var focusableSpec: Boolean? = null
    private var events: (EventScope.() -> Unit)? = null

    fun apply(
        observer: NodeObserver?,
        element: T,
        layout: LayoutBlock?,
        style: StyleBlock?,
        widget: (T.() -> Unit)?,
        classes: String?,
        id: String?,
        enabled: Boolean,
        visible: Boolean,
        focusable: Boolean?,
        events: (EventScope.() -> Unit)?,
    ) {
        this.observer = observer
        this.element = element
        this.layout = layout
        this.style = style
        this.widget = widget
        this.classSpec = classes
        this.idSpec = id
        this.enabledSpec = enabled
        this.visibleSpec = visible
        this.focusableSpec = focusable
        this.events = events
        write()
    }

    override fun reapply() {
        if (element != null) write()
    }

    // Only blocks can read state during apply, and observing costs ~1.5µs a node.
    private fun write() {
        val observer = this.observer
        if (observer == null || !hasBlocks()) writeNow() else observer.observe(this) { writeNow() }
    }

    private fun hasBlocks() = layout != null || style != null || widget != null || events != null

    private fun writeNow() {
        val element = this.element ?: return
        // Guarded one by one: a block that throws would otherwise stop the whole composition.
        guarded("layout") { layout?.let { TaffyLayoutStyleDsl(element.layout).it() } }
        guarded("style") { style?.let { element.style.it() } }
        // After the generic blocks, so a facade can see what they wrote (`Text` checks the height).
        guarded("widget style") { widget?.let { element.it() } }
        applyClasses(element, classSpec)
        applyFlags(element, idSpec, enabledSpec, visibleSpec, focusableSpec)
        guarded("events") { applyEvents(element, events) }
    }

    private inline fun guarded(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            LDLib2.LOGGER.error("A Compose {} block threw on {}", what, element, e)
        }
    }

    override fun onRemembered() = Unit

    override fun onForgotten() = release()

    override fun onAbandoned() = release()

    /**
     * Also takes this node's listeners and classes back off the element: a `remember`ed widget under
     * an `if` is re-emitted by a new node, and would otherwise keep firing stale handlers.
     */
    private fun release() {
        observer?.forget(this)
        observer = null
        element?.let { element ->
            for ((key, holder) in listeners) element.removeEventListener(key.type, holder, key.capture)
            for (name in classes) element.removeClass(name)
        }
        listeners.clear()
        classes = emptyList()
        lastClassSpec = null
        element = null
        layout = null
        style = null
        widget = null
        events = null
    }

    // Compared before splitting: the spec is usually a constant, so the common path allocates nothing.
    private fun applyClasses(element: UIElement, spec: String?) {
        if (spec == lastClassSpec) return
        lastClassSpec = spec
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
        for ((key, list) in next) {
            val holder = listeners.getOrPut(key) {
                EventHandlerHolder().also { element.addEventListener(key.type, it, key.capture) }
            }
            holder.handler = if (list.size == 1) list[0] else { event -> list.forEach { it(event) } }
        }
    }
}

/** Blank-tolerant, since `"card" + if (hot) " hot" else ""` easily leaves stray spaces. */
internal fun parseClasses(spec: String?): List<String> =
    spec?.split(' ')?.filter { it.isNotEmpty() }.orEmpty()

/** The keys to remove, then the keys to add; keys in both are left alone. */
internal fun <T> diffKeys(current: List<T>, next: List<T>): Pair<List<T>, List<T>> =
    current.filter { it !in next } to next.filter { it !in current }

/**
 * Active while a composition writes to the element tree. Widgets notify for those writes too
 * (`Slider#setRange` re-clamps, a `ToggleGroup` unchecks the previous toggle), so wrappers' change
 * listeners ignore what arrives meanwhile instead of writing state during apply. Render thread only.
 */
internal object CompositionWrites {
    @PublishedApi
    internal var depth = 0

    val active: Boolean get() = depth > 0

    inline fun <R> writing(block: () -> R): R {
        depth++
        try {
            return block()
        } finally {
            depth--
        }
    }
}

/**
 * Runs [block] with this facade's writes at `DEFAULT` origin, for values a wrapper picks on the
 * caller's behalf, so the caller's `INLINE` writes and stylesheets outrank them.
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

/** Drops the `INLINE` value of [property], so the stylesheet's or the default applies again. */
internal fun UIElement.removeInline(property: Property<*>) =
    styleBag.removeCandidates(property) { it.origin() == StyleOrigin.INLINE }
