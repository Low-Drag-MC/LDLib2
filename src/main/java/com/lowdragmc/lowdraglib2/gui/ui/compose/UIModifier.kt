package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Stable
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.style.Property

/**
 * A chain of adjustments to apply to an element — size, styling, classes, event handlers.
 *
 * <p>Usage matches Jetpack Compose: pass `UIModifier` for none, and build up from there.
 *
 * ```kotlin
 * Button(onClick = { ... }, modifier = UIModifier.width(80.px).padding(4.px)) { Text("ok") }
 * ```
 *
 * <p>A chain is a value: two chains built from the same calls compare equal, which is what lets a
 * recomposition tell that nothing about an element's appearance changed and skip the work.
 */
@Stable
interface UIModifier {
    /** The adjustments, in application order. Public so callers can write their own [ModifierOp]. */
    val ops: List<ModifierOp>

    fun then(other: UIModifier): UIModifier = when {
        other.ops.isEmpty() -> this
        ops.isEmpty() -> other
        else -> ChainedModifier(ops + other.ops)
    }

    /** The empty chain. */
    companion object : UIModifier {
        override val ops: List<ModifierOp> get() = emptyList()
        override fun toString() = "UIModifier"
    }
}

/** Appends a single adjustment. */
fun UIModifier.then(op: ModifierOp): UIModifier = ChainedModifier(ops + op)

internal class ChainedModifier(override val ops: List<ModifierOp>) : UIModifier {
    override fun equals(other: Any?) = other is UIModifier && ops == other.ops
    override fun hashCode() = ops.hashCode()
    override fun toString() = ops.joinToString(prefix = "UIModifier[", postfix = "]")
}

/**
 * One adjustment, and how to take it back off again.
 *
 * <p>[unapply] is not optional. When a recomposition drops an op from a chain, the element it was
 * applied to is being reused, not rebuilt — so whatever the op did has to be undone explicitly or it
 * lingers as a style nobody asked for.
 *
 * <p>Implementations must be values: equal ops mean "nothing to do".
 */
@Stable
interface ModifierOp {
    fun apply(element: UIElement)
    fun unapply(element: UIElement)
}

/**
 * Remembers which ops are currently on an element, so a new chain can be diffed against it.
 *
 * <p>Held by `remember` alongside the node rather than in a map keyed on the element: it then shares
 * the node's lifetime exactly, and there is nothing to clean up when the node leaves the composition.
 */
internal class AppliedModifier {
    private var current: List<ModifierOp> = emptyList()

    fun applyTo(element: UIElement, modifier: UIModifier) {
        val next = modifier.ops
        if (next == current) return
        val (toUnapply, toApply) = diffOps(current, next)
        for (op in toUnapply) op.unapply(element)
        for (op in toApply) op.apply(element)
        current = next
    }
}

/**
 * Splits a chain change into what has to come off and what has to go on.
 *
 * <p>Generic, and separate from [AppliedModifier], only so it can be tested: everything else in this
 * file names [UIElement], which puts it out of reach of `src/test` — that source set has no
 * Minecraft on its classpath.
 *
 * <p>Ops present in both lists are deliberately left alone rather than removed and re-added. That is
 * what keeps a registered [EventHandlerHolder] registered across recompositions instead of churning
 * listeners every frame.
 *
 * @return the ops to unapply, then the ops to apply, in that order
 */
internal fun <T> diffOps(current: List<T>, next: List<T>): Pair<List<T>, List<T>> =
    current.filter { it !in next } to next.filter { it !in current }

// region ops

/**
 * Writes a style property, and removes it again on [unapply].
 *
 * <p>Removal is what `set(property, null)` means to the style bag: it drops this element's own
 * candidate for the property, leaving whatever the stylesheets said to win again. That is the
 * correct undo — not "write the default back", which would out-rank the stylesheet.
 *
 * @param onLayout which of the element's two style facades to write through. They share one bag, so
 *   this changes nothing mechanically; it keeps the call sites honest about what is being set.
 */
data class StyleOp<T : Any>(
    val property: Property<T>,
    val value: T,
    val onLayout: Boolean,
) : ModifierOp {
    override fun apply(element: UIElement) {
        facade(element).set(property, value)
    }

    override fun unapply(element: UIElement) {
        facade(element).set(property, null)
    }

    private fun facade(element: UIElement) = if (onLayout) element.layout else element.style
}

internal fun <T : Any> UIModifier.layoutProp(property: Property<T>, value: T): UIModifier =
    then(StyleOp(property, value, onLayout = true))

internal fun <T : Any> UIModifier.styleProp(property: Property<T>, value: T): UIModifier =
    then(StyleOp(property, value, onLayout = false))

/** Adds a CSS-style class, for stylesheet selectors to match on. */
data class ClassOp(val name: String) : ModifierOp {
    override fun apply(element: UIElement) {
        element.addClasses(name)
    }

    override fun unapply(element: UIElement) {
        element.removeClass(name)
    }
}

/**
 * Sets the element's id.
 *
 * <p>Only useful before the element is attached — [com.lowdragmc.lowdraglib2.gui.ui.ModularUI]
 * indexes ids at registration time and [UIElement.setId] does not update that index afterwards. It
 * still works for `#id` stylesheet selectors, which re-match on change.
 */
data class IdOp(val id: String) : ModifierOp {
    override fun apply(element: UIElement) {
        element.id = id
    }

    override fun unapply(element: UIElement) {
        element.id = ""
    }
}

/**
 * Holds the current handler for an [EventOp].
 *
 * <p>The indirection is the point. Event lambdas capture composition state, so they are a different
 * instance on every recomposition; registering them directly would mean an
 * `addEventListener`/`removeEventListener` pair per frame — and since listeners are prepended, that
 * would also quietly reshuffle dispatch order. Instead the *holder* is registered once and remembered,
 * and recomposition only swaps what it points at.
 */
class EventHandlerHolder : UIEventListener {
    @JvmField
    var handler: ((UIEvent) -> Unit)? = null

    override fun handleEvent(event: UIEvent) {
        handler?.invoke(event)
    }
}

/**
 * Greys an element out and stops it responding.
 *
 * <p>Not a style property but a flag on the element, which is why it needs its own op: widgets check
 * it before acting on input, and it also adds the `__disabled__` class that stylesheets key their
 * disabled appearance off.
 */
data class ActiveOp(val active: Boolean) : ModifierOp {
    override fun apply(element: UIElement) {
        element.setActive(active)
    }

    override fun unapply(element: UIElement) {
        element.setActive(true)
    }
}

/** Hides an element while leaving it in the layout. */
data class VisibleOp(val visible: Boolean) : ModifierOp {
    override fun apply(element: UIElement) {
        element.setVisible(visible)
    }

    override fun unapply(element: UIElement) {
        element.setVisible(true)
    }
}

/** Whether the element can take keyboard focus. */
data class FocusableOp(val focusable: Boolean) : ModifierOp {
    override fun apply(element: UIElement) {
        element.setFocusable(focusable)
    }

    override fun unapply(element: UIElement) {
        element.setFocusable(false)
    }
}

/** Registers [holder] for [type]. Equal by holder identity, which `remember` keeps stable. */
data class EventOp(
    val type: String,
    val capture: Boolean,
    val holder: EventHandlerHolder,
) : ModifierOp {
    override fun apply(element: UIElement) {
        element.addEventListener(type, holder, capture)
    }

    override fun unapply(element: UIElement) {
        element.removeEventListener(type, holder, capture)
    }
}

// endregion
