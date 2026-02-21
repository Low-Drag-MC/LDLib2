package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider
import java.util.function.Consumer

/**
 * Extension function for Selector.SelectorStyle DSL
 */
fun <T> Selector<T>.selectorStyleDsl(init: Selector<T>.SelectorStyle.() -> Unit = {}): Selector<T> {
    this.selectorStyle.apply(init)
    return this
}

/**
 * Specification for Selector element
 */
open class SelectorSpec<T, E : Selector<T>>(
    var selectorStyle: (Selector<T>.SelectorStyle.() -> Unit)? = null,
    var candidates: List<T>? = null,
    var candidateUIProvider: UIElementProvider<T>? = null,
    var selectedValue: T? = null,
    var onValueChanged: Consumer<T>? = null,
) : ElementSpec<E>() {
    /**
     * Set candidates list
     */
    fun candidates(vararg items: T) = apply {
        this.candidates = items.toList()
    }

    /**
     * Set candidates list
     */
    fun candidates(items: List<T>) = apply {
        this.candidates = items
    }

    /**
     * Set candidate UI provider
     */
    fun candidateUI(provider: UIElementProvider<T>) = apply {
        this.candidateUIProvider = provider
    }

    /**
     * Set selected value
     */
    fun selected(value: T) = apply {
        this.selectedValue = value
    }

    /**
     * Set value change listener
     */
    fun onChange(handler: Consumer<T>) = apply {
        this.onValueChanged = handler
    }

    /**
     * Set value change listener (Kotlin lambda)
     */
    fun onChange(handler: (T) -> Unit) = apply {
        this.onValueChanged = Consumer { handler(it) }
    }

}

/**
 * Selector element builder
 */
open class SelectorElement<T, E : Selector<T>>(
    element: E,
    spec: (SelectorSpec<T, E>.() -> Unit)? = null,
) : UIContainer<E, SelectorSpec<T, E>>(element, spec) {
    override fun makeSpec(): SelectorSpec<T, E>? {
        return spec?.let { SelectorSpec<T, E>().apply(it) }
    }

    override fun build(spec: SelectorSpec<T, E>?): E {
        val e = super.build(spec)
        applySelectorProperties(spec, e)
        return e
    }

    protected fun applySelectorProperties(spec: SelectorSpec<T, E>?, element: Selector<T>) {
        spec?.selectorStyle?.let(element.selectorStyle::apply)
        spec?.candidateUIProvider?.let { element.setCandidateUIProvider(it) }
        spec?.candidates?.let { element.setCandidates(it) }
        spec?.selectedValue?.let { element.setSelected(it, false) }
        spec?.onValueChanged?.let { element.setOnValueChanged(it) }
    }
}

/**
 * Top Level - Create a standalone Selector element
 */
fun <T> selector(spec: (SelectorSpec<T, Selector<T>>.() -> Unit)? = null,
                 init: SelectorElement<T, Selector<T>>.() -> Unit = {}): Selector<T> {
    return SelectorElement(Selector<T>(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Selector as a child to a container
 */
fun <T> UIContainer<*, *>.selector(spec: (SelectorSpec<T, Selector<T>>.() -> Unit)? = null,
                                    init: SelectorElement<T, Selector<T>>.() -> Unit = {}) =
    add(SelectorElement(Selector<T>(), spec), init)

/**
 * DSL converter - Convert existing Selector to DSL builder
 */
fun <T> Selector<T>.dsl(spec: (SelectorSpec<T, Selector<T>>.() -> Unit)? = null,
                        init: SelectorElement<T, Selector<T>>.() -> Unit = {}): SelectorElement<T, Selector<T>> {
    return SelectorElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set candidates
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.withCandidates(vararg items: T): SelectorElement<T, E> = apply {
    element.setCandidates(items.toList())
}

/**
 * Extension: Set candidates list
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.withCandidates(items: List<T>): SelectorElement<T, E> = apply {
    element.setCandidates(items)
}

/**
 * Extension: Set candidate UI provider
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.withCandidateUI(provider: UIElementProvider<T>): SelectorElement<T, E> = apply {
    element.setCandidateUIProvider(provider)
}

/**
 * Extension: Set selected value
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.withSelected(value: T): SelectorElement<T, E> = apply {
    element.setSelected(value)
}

/**
 * Extension: Set value change listener
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.onValueChange(handler: (T) -> Unit): SelectorElement<T, E> = apply {
    element.setOnValueChanged(Consumer { handler(it) })
}

/**
 * Extension: Show overlay on hover
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.withOverlay(): SelectorElement<T, E> = apply {
    element.selectorStyle.showOverlay(true)
}

/**
 * Extension: Close after selection
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.closeOnSelect(): SelectorElement<T, E> = apply {
    element.selectorStyle.closeAfterSelect(true)
}

/**
 * Extension: Set max item count before scrolling
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.withMaxItems(count: Int): SelectorElement<T, E> = apply {
    element.selectorStyle.maxItemCount(count)
}

/**
 * Extension: Set scroller view height
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.withScrollHeight(height: Float): SelectorElement<T, E> = apply {
    element.selectorStyle.scrollerViewHeight(height)
}

/**
 * Extension: Show dropdown
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.show(): SelectorElement<T, E> = apply {
    element.show()
}

/**
 * Extension: Hide dropdown
 */
fun <T, E : Selector<T>> SelectorElement<T, E>.hide(): SelectorElement<T, E> = apply {
    element.hide()
}