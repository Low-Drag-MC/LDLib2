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
 * <p>Each `(type, capture)` pair is registered on the element once, however many handlers are
 * declared for it and however often the block re-runs; declaring the same event twice runs both.
 */
class EventScope internal constructor() {
    internal val handlers = LinkedHashMap<EventKey, MutableList<(UIEvent) -> Unit>>()

    internal fun reset() = handlers.clear()

    /**
     * Any event type, including ones with no named helper below.
     *
     * <p>Declaring the same event twice adds a second handler rather than replacing the first, which
     * is what lets [track] coexist with a caller's own `onMouseEnter`. Handlers do not accumulate
     * across recompositions — the block is re-run from empty each time.
     */
    fun on(type: String, capture: Boolean = false, handler: (UIEvent) -> Unit) {
        handlers.getOrPut(EventKey(type, capture)) { ArrayList(1) }.add(handler)
    }

    /** Press and release on the same element. Distinct from `Button`'s own mouse-down `onClick`. */
    fun onClick(handler: (UIEvent) -> Unit) = on(UIEvents.CLICK, handler = handler)
    fun onMouseDown(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_DOWN, handler = handler)
    fun onMouseUp(handler: (UIEvent) -> Unit) = on(UIEvents.MOUSE_UP, handler = handler)
    /**
     * Capture phase, and not optionally so: `ModularUI` dispatches enter and leave with
     * `hasBubblePhase = false`, so a bubble-phase listener for them can never fire. It also targets
     * the innermost element under the pointer, so capture is what lets a container hear about its
     * own children being hovered — which is the only thing anyone wants from these two.
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
 *
 * <p>The blocks run inside a [NodeObserver], so state they read invalidates *this node* and nothing
 * else — see that class for why the obvious alternative does not work. Which means the last set of
 * blocks has to be kept here: a re-apply is driven by a state change, with no composition running to
 * supply them again.
 */
internal class AppliedProps<T : UIElement> : RememberObserver, Reappliable {
    private var classes: List<String> = emptyList()
    private var lastClassSpec: String? = null
    private var id: String? = null

    // Null until first written, so the first apply always writes: a factory may hand over an element
    // that starts disabled or hidden, and assuming the defaults would never correct it.
    private var enabled: Boolean? = null
    private var visible: Boolean? = null
    private var focusable: Boolean? = null
    private val listeners = HashMap<EventKey, EventHandlerHolder>()
    private val scope = EventScope()

    // The last-applied arguments, kept so [reapply] can run without a composition.
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

    /**
     * Observed only when the caller actually supplied a block.
     *
     * <p>Everything else here is an already-evaluated value — the classes string, the flags — computed
     * in the composable body where Compose tracked the reads itself. With no block there is nothing
     * that *could* read state during apply, and observing anyway would cost a scope's worth of
     * bookkeeping per node for no possible benefit. Measured at ~1.5µs a node, which is the whole
     * difference between the benchmark's 0.8ms and 1.6ms for a 500-row update.
     */
    private fun write() {
        val observer = this.observer
        if (observer == null || !hasBlocks()) writeNow() else observer.observe(this) { writeNow() }
    }

    private fun hasBlocks() = layout != null || style != null || widget != null || events != null

    private fun writeNow() {
        val element = this.element ?: return
        // The caller's blocks are guarded one by one: a block that throws would otherwise escape into
        // the recomposer and stop the whole composition, for the sake of one node's appearance.
        guarded("layout") { layout?.let { TaffyLayoutStyleDsl(element.layout).it() } }
        guarded("style") { style?.let { element.style.it() } }
        // After the generic blocks, so a widget facade can look at what they wrote — that is how
        // `Text` decides whether the caller gave it a height.
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
     * Stops the observer holding this node's reads, and this node holding its element.
     *
     * <p>Also takes this node's listeners and classes back off the element. Usually the element is
     * being discarded anyway, but not always: a widget kept in `remember` and handed to a node under
     * an `if` is re-emitted by a *new* node when the condition comes back, and anything left behind
     * here would still be attached to it, firing with state captured long ago.
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

    /**
     * Compares the spec string before splitting it.
     *
     * <p>`classes` is a constant at almost every call site, so the string compare is a reference hit
     * and the common path allocates nothing. Splitting first instead — which is the obvious way to
     * write this — costs a list and a substring per node per apply, which showed up as roughly half a
     * millisecond across a 500-row update.
     */
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
            // The single-handler case is the common one, and gets no wrapper allocation.
            holder.handler = if (list.size == 1) list[0] else { event -> list.forEach { it(event) } }
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
 * Whether the element tree is being written by a composition right now, rather than acted on by the
 * user.
 *
 * <p>LDLib2's widgets report a change the same way whoever made it: `Slider#setRange` re-clamps and
 * notifies, a `ToggleGroup` unchecks the previous toggle with notification on, a removed toggle hands
 * its checked state to a sibling. Those are consequences of what the composition just did, not edits
 * to report back to it — and reporting them would write state during the apply phase. So everything a
 * composition does to the tree runs inside [writing], and the wrappers' change listeners ignore what
 * arrives while it is active.
 *
 * <p>Render thread only, like everything else here, so a plain counter is enough.
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
