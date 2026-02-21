package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import java.util.function.Function

/**
 * Extension function for Scroller.ScrollerStyle DSL
 */
fun <T : Scroller> T.scrollerStyleDsl(init: Scroller.ScrollerStyle.() -> Unit = {}): T {
    this.scrollerStyle.apply(init)
    return this
}

/**
 * Base specification for Scroller elements
 */
open class ScrollerSpec<T : Scroller>(
    var scrollerStyle: (Scroller.ScrollerStyle.() -> Unit)? = null,
    var minValue: Float? = null,
    var maxValue: Float? = null,
    var value: Float? = null,
    var normalizedValue: Float? = null,
    var scrollDelta: Float? = null,
    var scrollBarSize: Float? = null,
    var onValueChanged: ((Float) -> Unit)? = null,
    var clampNormalizedValue: Function<Float, Float>? = null,
) : ElementSpec<T>() {
    /**
     * Set the range of the scroller
     */
    fun range(min: Float, max: Float) = apply {
        this.minValue = min
        this.maxValue = max
    }

    /**
     * Set the scroll delta (how much to scroll per step)
     */
    fun delta(delta: Float) = apply {
        this.scrollDelta = delta
    }

    /**
     * Set scroll bar size as percentage (0-100)
     */
    fun barSize(size: Float) = apply {
        this.scrollBarSize = size
    }

    /**
     * Set value change listener
     */
    fun onChange(handler: (Float) -> Unit) = apply {
        this.onValueChanged = handler
    }
}

/**
 * Base Scroller element builder
 */
open class ScrollerElement<T : Scroller>(
    element: T,
    spec: (ScrollerSpec<T>.() -> Unit)? = null,
) : UIContainer<T, ScrollerSpec<T>>(element, spec) {
    override fun makeSpec(): ScrollerSpec<T>? {
        return spec?.let { ScrollerSpec<T>().apply(it) }
    }

    override fun build(spec: ScrollerSpec<T>?): T {
        val e = super.build(spec)
        applyScrollerProperties(spec, e)
        return e
    }

    protected fun applyScrollerProperties(spec: ScrollerSpec<T>?, element: Scroller) {
        spec?.scrollerStyle?.let(element.scrollerStyle::apply)

        // Apply range first if both min and max are specified
        if (spec?.minValue != null || spec?.maxValue != null) {
            val min = spec.minValue ?: element.minValue
            val max = spec.maxValue ?: element.maxValue
            element.setRange(min, max)
        }

        // Apply value or normalizedValue
        if (spec?.normalizedValue != null) {
            spec.normalizedValue?.let { element.setNormalizedValue(it) }
        } else {
            spec?.value?.let { element.setValue(it) }
        }

        spec?.scrollDelta?.let { element.scrollerStyle.scrollDelta(it) }
        spec?.scrollBarSize?.let { element.scrollerStyle.scrollBarSize(it) }
        spec?.onValueChanged?.let { handler ->
            element.setOnValueChanged { handler(it) }
        }
        spec?.clampNormalizedValue?.let { element.setClampNormalizedValue(it) }
    }
}

// ============================================
// Horizontal Scroller (scroller-horizontal)
// ============================================

/**
 * Specification for Horizontal Scroller
 */
open class ScrollerHorizontalSpec : ScrollerSpec<Scroller.Horizontal>()

/**
 * Horizontal Scroller element builder
 */
open class ScrollerHorizontalElement(
    element: Scroller.Horizontal,
    spec: (ScrollerSpec<Scroller.Horizontal>.() -> Unit)? = null,
) : ScrollerElement<Scroller.Horizontal>(element, spec) {
    override fun makeSpec(): ScrollerHorizontalSpec? {
        return spec?.let { ScrollerHorizontalSpec().apply(it) }
    }
}

/**
 * Top Level - Create a standalone Horizontal Scroller
 */
fun scrollerHorizontal(spec: (ScrollerSpec<Scroller.Horizontal>.() -> Unit)? = null,
                       init: ScrollerHorizontalElement.() -> Unit = {}): Scroller.Horizontal {
    return ScrollerHorizontalElement(Scroller.Horizontal(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Horizontal Scroller as a child to a container
 */
fun UIContainer<*, *>.scrollerHorizontal(spec: (ScrollerSpec<Scroller.Horizontal>.() -> Unit)? = null,
                                         init: ScrollerHorizontalElement.() -> Unit = {}) =
    add(ScrollerHorizontalElement(Scroller.Horizontal(), spec), init)

/**
 * DSL converter - Convert existing Horizontal Scroller to DSL builder
 */
fun Scroller.Horizontal.dsl(spec: (ScrollerSpec<Scroller.Horizontal>.() -> Unit)? = null,
                            init: ScrollerHorizontalElement.() -> Unit = {}): ScrollerHorizontalElement {
    return ScrollerHorizontalElement(this, spec).apply(init)
}

// ============================================
// Vertical Scroller (scroller-vertical)
// ============================================

/**
 * Specification for Vertical Scroller
 */
open class ScrollerVerticalSpec : ScrollerSpec<Scroller.Vertical>()

/**
 * Vertical Scroller element builder
 */
open class ScrollerVerticalElement(
    element: Scroller.Vertical,
    spec: (ScrollerSpec<Scroller.Vertical>.() -> Unit)? = null,
) : ScrollerElement<Scroller.Vertical>(element, spec) {
    override fun makeSpec(): ScrollerVerticalSpec? {
        return spec?.let { ScrollerVerticalSpec().apply(it) }
    }
}

/**
 * Top Level - Create a standalone Vertical Scroller
 */
fun scrollerVertical(spec: (ScrollerSpec<Scroller.Vertical>.() -> Unit)? = null,
                     init: ScrollerVerticalElement.() -> Unit = {}): Scroller.Vertical {
    return ScrollerVerticalElement(Scroller.Vertical(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Vertical Scroller as a child to a container
 */
fun UIContainer<*, *>.scrollerVertical(spec: (ScrollerSpec<Scroller.Vertical>.() -> Unit)? = null,
                                       init: ScrollerVerticalElement.() -> Unit = {}) =
    add(ScrollerVerticalElement(Scroller.Vertical(), spec), init)

/**
 * DSL converter - Convert existing Vertical Scroller to DSL builder
 */
fun Scroller.Vertical.dsl(spec: (ScrollerSpec<Scroller.Vertical>.() -> Unit)? = null,
                          init: ScrollerVerticalElement.() -> Unit = {}): ScrollerVerticalElement {
    return ScrollerVerticalElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set scroller range
 */
fun <T : Scroller> ScrollerElement<T>.withRange(min: Float, max: Float): ScrollerElement<T> = apply {
    element.setRange(min, max)
}

/**
 * Extension: Set scroller value
 */
fun <T : Scroller> ScrollerElement<T>.withValue(value: Float): ScrollerElement<T> = apply {
    element.setValue(value)
}

/**
 * Extension: Set normalized value (0-1)
 */
fun <T : Scroller> ScrollerElement<T>.withNormalizedValue(value: Float): ScrollerElement<T> = apply {
    element.setNormalizedValue(value)
}

/**
 * Extension: Set scroll delta
 */
fun <T : Scroller> ScrollerElement<T>.withScrollDelta(delta: Float): ScrollerElement<T> = apply {
    element.scrollerStyle.scrollDelta(delta)
}

/**
 * Extension: Set scroll bar size
 */
fun <T : Scroller> ScrollerElement<T>.withScrollBarSize(size: Float): ScrollerElement<T> = apply {
    element.scrollerStyle.scrollBarSize(size)
}

/**
 * Extension: Set value change callback
 */
fun <T : Scroller> ScrollerElement<T>.onValueChange(handler: (Float) -> Unit): ScrollerElement<T> = apply {
    element.setOnValueChanged { handler(it) }
}

/**
 * Extension: Set clamp function for normalized value
 */
fun <T : Scroller> ScrollerElement<T>.withClamp(clamp: Function<Float, Float>): ScrollerElement<T> = apply {
    element.setClampNormalizedValue(clamp)
}
