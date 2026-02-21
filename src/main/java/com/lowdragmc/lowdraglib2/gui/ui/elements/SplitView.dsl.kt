package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement

/**
 * Specification for SplitView elements
 */
open class SplitViewSpec<T : SplitView>(
    var percentage: Float? = null,
    var borderSize: Float? = null,
    var minPercentage: Float? = null,
    var maxPercentage: Float? = null,
    var firstPane: UIElement? = null,
    var secondPane: UIElement? = null,
) : ElementSpec<T>() {
    /**
     * Set the split percentage (0-100)
     */
    fun split(percentage: Float) = apply {
        this.percentage = percentage
    }

    /**
     * Set min/max percentage constraints
     */
    fun range(min: Float, max: Float) = apply {
        this.minPercentage = min
        this.maxPercentage = max
    }
}

/**
 * Base SplitView element builder
 */
open class SplitViewElement<T : SplitView>(
    element: T,
    spec: (SplitViewSpec<T>.() -> Unit)? = null,
) : UIContainer<T, SplitViewSpec<T>>(element, spec) {
    override fun makeSpec(): SplitViewSpec<T>? {
        return spec?.let { SplitViewSpec<T>().apply(it) }
    }

    override fun build(spec: SplitViewSpec<T>?): T {
        val e = super.build(spec)
        applySplitViewProperties(spec, e)
        return e
    }

    protected fun applySplitViewProperties(spec: SplitViewSpec<T>?, element: SplitView) {
        spec?.borderSize?.let { element.setBorderSize(it) }
        spec?.minPercentage?.let { element.setMinPercentage(it) }
        spec?.maxPercentage?.let { element.setMaxPercentage(it) }
        spec?.firstPane?.let { element.first(it) }
        spec?.secondPane?.let { element.second(it) }
        spec?.percentage?.let { element.setPercentage(it) }
    }
}

// ============================================
// Horizontal SplitView (split-view-horizontal)
// ============================================

/**
 * Specification for Horizontal SplitView
 */
open class SplitViewHorizontalSpec(
    var leftPane: UIElement? = null,
    var rightPane: UIElement? = null,
) : SplitViewSpec<SplitView.Horizontal>() {
    /**
     * Configure left pane
     */
    fun left(pane: UIElement) = apply {
        this.leftPane = pane
        this.firstPane = pane
    }

    /**
     * Configure right pane
     */
    fun right(pane: UIElement) = apply {
        this.rightPane = pane
        this.secondPane = pane
    }
}

/**
 * Horizontal SplitView element builder
 */
open class SplitViewHorizontalElement(
    element: SplitView.Horizontal,
    spec: (SplitViewHorizontalSpec.() -> Unit)? = null,
) : SplitViewElement<SplitView.Horizontal>(element, spec as (SplitViewSpec<SplitView.Horizontal>.() -> Unit)?) {
    override fun makeSpec(): SplitViewHorizontalSpec? {
        return spec?.let { SplitViewHorizontalSpec().apply(it) }
    }

    override fun build(spec: SplitViewSpec<SplitView.Horizontal>?): SplitView.Horizontal {
        val e = super.build(spec)
        if (spec is SplitViewHorizontalSpec) {
            spec.leftPane?.let { e.left(it) }
            spec.rightPane?.let { e.right(it) }
        }
        return e
    }
}

/**
 * Top Level - Create a standalone Horizontal SplitView
 */
fun splitViewHorizontal(spec: (SplitViewHorizontalSpec.() -> Unit)? = null,
                        init: SplitViewHorizontalElement.() -> Unit = {}): SplitView.Horizontal {
    return SplitViewHorizontalElement(SplitView.Horizontal(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Horizontal SplitView as a child to a container
 */
fun UIContainer<*, *>.splitViewHorizontal(spec: (SplitViewHorizontalSpec.() -> Unit)? = null,
                                          init: SplitViewHorizontalElement.() -> Unit = {}) =
    add(SplitViewHorizontalElement(SplitView.Horizontal(), spec), init)

/**
 * DSL converter - Convert existing Horizontal SplitView to DSL builder
 */
fun SplitView.Horizontal.dsl(spec: (SplitViewHorizontalSpec.() -> Unit)? = null,
                             init: SplitViewHorizontalElement.() -> Unit = {}): SplitViewHorizontalElement {
    return SplitViewHorizontalElement(this, spec).apply(init)
}

// ============================================
// Vertical SplitView (split-view-vertical)
// ============================================

/**
 * Specification for Vertical SplitView
 */
open class SplitViewVerticalSpec(
    var topPane: UIElement? = null,
    var bottomPane: UIElement? = null,
) : SplitViewSpec<SplitView.Vertical>() {
    /**
     * Configure top pane
     */
    fun top(pane: UIElement) = apply {
        this.topPane = pane
        this.firstPane = pane
    }

    /**
     * Configure bottom pane
     */
    fun bottom(pane: UIElement) = apply {
        this.bottomPane = pane
        this.secondPane = pane
    }
}

/**
 * Vertical SplitView element builder
 */
open class SplitViewVerticalElement(
    element: SplitView.Vertical,
    spec: (SplitViewVerticalSpec.() -> Unit)? = null,
) : SplitViewElement<SplitView.Vertical>(element, spec as (SplitViewSpec<SplitView.Vertical>.() -> Unit)?) {
    override fun makeSpec(): SplitViewVerticalSpec? {
        return spec?.let { SplitViewVerticalSpec().apply(it) }
    }

    override fun build(spec: SplitViewSpec<SplitView.Vertical>?): SplitView.Vertical {
        val e = super.build(spec)
        if (spec is SplitViewVerticalSpec) {
            spec.topPane?.let { e.top(it) }
            spec.bottomPane?.let { e.bottom(it) }
        }
        return e
    }
}

/**
 * Top Level - Create a standalone Vertical SplitView
 */
fun splitViewVertical(spec: (SplitViewVerticalSpec.() -> Unit)? = null,
                      init: SplitViewVerticalElement.() -> Unit = {}): SplitView.Vertical {
    return SplitViewVerticalElement(SplitView.Vertical(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Vertical SplitView as a child to a container
 */
fun UIContainer<*, *>.splitViewVertical(spec: (SplitViewVerticalSpec.() -> Unit)? = null,
                                        init: SplitViewVerticalElement.() -> Unit = {}) =
    add(SplitViewVerticalElement(SplitView.Vertical(), spec), init)

/**
 * DSL converter - Convert existing Vertical SplitView to DSL builder
 */
fun SplitView.Vertical.dsl(spec: (SplitViewVerticalSpec.() -> Unit)? = null,
                           init: SplitViewVerticalElement.() -> Unit = {}): SplitViewVerticalElement {
    return SplitViewVerticalElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set left pane for horizontal split
 */
fun SplitViewHorizontalElement.withLeft(pane: UIElement): SplitViewHorizontalElement = apply {
    element.left(pane)
}

/**
 * Extension: Set right pane for horizontal split
 */
fun SplitViewHorizontalElement.withRight(pane: UIElement): SplitViewHorizontalElement = apply {
    element.right(pane)
}

/**
 * Extension: Set top pane for vertical split
 */
fun SplitViewVerticalElement.withTop(pane: UIElement): SplitViewVerticalElement = apply {
    element.top(pane)
}

/**
 * Extension: Set bottom pane for vertical split
 */
fun SplitViewVerticalElement.withBottom(pane: UIElement): SplitViewVerticalElement = apply {
    element.bottom(pane)
}

/**
 * Extension: Set split percentage
 */
fun <T : SplitView> SplitViewElement<T>.withPercentage(percentage: Float): SplitViewElement<T> = apply {
    element.setPercentage(percentage)
}

/**
 * Extension: Set border size
 */
fun <T : SplitView> SplitViewElement<T>.withBorderSize(size: Float): SplitViewElement<T> = apply {
    element.setBorderSize(size)
}

/**
 * Extension: Set percentage range
 */
fun <T : SplitView> SplitViewElement<T>.withRange(min: Float, max: Float): SplitViewElement<T> = apply {
    element.setMinPercentage(min)
    element.setMaxPercentage(max)
}
