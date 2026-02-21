package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import java.util.function.Consumer

/**
 * Specification for BindableValue element
 */
open class BindableValueSpec<T, E : BindableValue<T>>(
    var initialValue: T? = null,
    var onValueChanged: Consumer<T>? = null,
) : ElementSpec<E>() {
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
 * BindableValue element builder
 */
open class BindableValueElement<T, E : BindableValue<T>>(
    element: E,
    spec: (BindableValueSpec<T, E>.() -> Unit)? = null,
) : UIContainer<E, BindableValueSpec<T, E>>(element, spec) {
    override fun makeSpec(): BindableValueSpec<T, E>? {
        return spec?.let { BindableValueSpec<T, E>().apply(it) }
    }

    override fun build(spec: BindableValueSpec<T, E>?): E {
        val e = super.build(spec)
        applyBindableValueProperties(spec, e)
        return e
    }

    protected fun applyBindableValueProperties(spec: BindableValueSpec<T, E>?, element: BindableValue<T>) {
        spec?.initialValue?.let { element.setValue(it, false) }
        spec?.onValueChanged?.let { element.registerValueListener(it) }
    }
}

/**
 * Top Level - Create a standalone BindableValue element
 */
fun <T> bindableValue(initialValue: T? = null,
                      spec: (BindableValueSpec<T, BindableValue<T>>.() -> Unit)? = null,
                      init: BindableValueElement<T, BindableValue<T>>.() -> Unit = {}): BindableValue<T> {
    return BindableValueElement(BindableValue(initialValue), spec).apply(init).build()
}

/**
 * Internal Builder - Add BindableValue as a child to a container
 */
fun <T> UIContainer<*, *>.bindableValue(initialValue: T? = null,
                                         spec: (BindableValueSpec<T, BindableValue<T>>.() -> Unit)? = null,
                                         init: BindableValueElement<T, BindableValue<T>>.() -> Unit = {}) =
    add(BindableValueElement(BindableValue(initialValue), spec), init)

/**
 * DSL converter - Convert existing BindableValue to DSL builder
 */
fun <T> BindableValue<T>.dsl(spec: (BindableValueSpec<T, BindableValue<T>>.() -> Unit)? = null,
                             init: BindableValueElement<T, BindableValue<T>>.() -> Unit = {}): BindableValueElement<T, BindableValue<T>> {
    return BindableValueElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set value
 */
fun <T, E : BindableValue<T>> BindableValueElement<T, E>.withValue(value: T): BindableValueElement<T, E> = apply {
    element.setValue(value)
}

/**
 * Extension: Set value change listener
 */
fun <T, E : BindableValue<T>> BindableValueElement<T, E>.onValueChange(handler: (T) -> Unit): BindableValueElement<T, E> = apply {
    element.registerValueListener(Consumer { handler(it) })
}

/**
 * Extension: Get current value
 */
fun <T, E : BindableValue<T>> BindableValueElement<T, E>.getValue(): T {
    return element.value
}

