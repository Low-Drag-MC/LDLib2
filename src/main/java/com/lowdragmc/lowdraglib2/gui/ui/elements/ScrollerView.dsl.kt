package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIBuilder
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode
import java.util.function.Consumer

/**
 * Extension function for ScrollerView.ScrollerViewStyle DSL
 */
fun <T : ScrollerView> T.scrollerViewStyleDsl(init: ScrollerView.ScrollerViewStyle.() -> Unit = {}): T {
    this.scrollerViewStyle.apply(init)
    return this
}

/**
 * Specification for ScrollerView element
 */
open class ScrollerViewSpec<T : ScrollerView>(
    var scrollerViewStyle: (ScrollerView.ScrollerViewStyle.() -> Unit)? = null,
) : ElementSpec<T>()

/**
 * ScrollerView element builder
 */
open class ScrollerViewElement<T : ScrollerView>(
    element: T,
    spec: (ScrollerViewSpec<T>.() -> Unit)? = null,
) : UIContainer<T, ScrollerViewSpec<T>>(element, spec) {
    override fun makeSpec(): ScrollerViewSpec<T>? {
        return spec?.let { ScrollerViewSpec<T>().apply(it) }
    }

    override fun build(spec: ScrollerViewSpec<T>?): T {
        val e = super.build(spec)
        applyScrollerViewProperties(spec, e)
        return e
    }

    protected fun applyScrollerViewProperties(spec: ScrollerViewSpec<T>?, element: ScrollerView) {
        spec?.scrollerViewStyle?.let(element.scrollerViewStyle::apply)
    }

    /**
     * Override addChild to add children to the scroll view container
     */
    override fun addChild(child: UIBuilder<*>) {
        element.addScrollViewChild(child.build())
    }
}

/**
 * Top Level - Create a standalone ScrollerView element
 */
fun scrollerView(spec: (ScrollerViewSpec<ScrollerView>.() -> Unit)? = null,
                 init: ScrollerViewElement<ScrollerView>.() -> Unit = {}): ScrollerView {
    return ScrollerViewElement(ScrollerView(), spec).apply(init).build()
}

/**
 * Internal Builder - Add ScrollerView as a child to a container
 */
fun UIContainer<*, *>.scrollerView(spec: (ScrollerViewSpec<ScrollerView>.() -> Unit)? = null,
                                    init: ScrollerViewElement<ScrollerView>.() -> Unit = {}) =
    add(ScrollerViewElement(ScrollerView(), spec), init)

/**
 * DSL converter - Convert existing ScrollerView to DSL builder
 */
fun <T : ScrollerView> T.dsl(spec: (ScrollerViewSpec<T>.() -> Unit)? = null,
                             init: ScrollerViewElement<T>.() -> Unit = {}): ScrollerViewElement<T> {
    return ScrollerViewElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Configure view container
 */
fun <T : ScrollerView> ScrollerViewElement<T>.withViewContainer(config: UIElement.() -> Unit): ScrollerViewElement<T> = apply {
    element.viewContainer(Consumer { config(it) })
}

/**
 * Extension: Configure view port
 */
fun <T : ScrollerView> ScrollerViewElement<T>.withViewPort(config: UIElement.() -> Unit): ScrollerViewElement<T> = apply {
    element.viewPort(Consumer { config(it) })
}

/**
 * Extension: Configure vertical scroller
 */
fun <T : ScrollerView> ScrollerViewElement<T>.withVerticalScroller(config: Scroller.() -> Unit): ScrollerViewElement<T> = apply {
    element.verticalScroller(Consumer { config(it) })
}

/**
 * Extension: Configure horizontal scroller
 */
fun <T : ScrollerView> ScrollerViewElement<T>.withHorizontalScroller(config: Scroller.() -> Unit): ScrollerViewElement<T> = apply {
    element.horizontalScroller(Consumer { config(it) })
}

/**
 * Extension: Set to vertical-only scrolling
 */
fun <T : ScrollerView> ScrollerViewElement<T>.verticalOnly(): ScrollerViewElement<T> = apply {
    element.scrollerViewStyle.mode(ScrollerMode.VERTICAL)
}

/**
 * Extension: Set to horizontal-only scrolling
 */
fun <T : ScrollerView> ScrollerViewElement<T>.horizontalOnly(): ScrollerViewElement<T> = apply {
    element.scrollerViewStyle.mode(ScrollerMode.HORIZONTAL)
}

/**
 * Extension: Enable both directions
 */
fun <T : ScrollerView> ScrollerViewElement<T>.bothDirections(): ScrollerViewElement<T> = apply {
    element.scrollerViewStyle.mode(ScrollerMode.BOTH)
}

/**
 * Extension: Auto-hide scrollbars
 */
fun <T : ScrollerView> ScrollerViewElement<T>.autoScrollbars(): ScrollerViewElement<T> = apply {
    element.scrollerViewStyle.verticalScrollDisplay(ScrollDisplay.AUTO)
    element.scrollerViewStyle.horizontalScrollDisplay(ScrollDisplay.AUTO)
}

/**
 * Extension: Always show scrollbars
 */
fun <T : ScrollerView> ScrollerViewElement<T>.alwaysShowScrollbars(): ScrollerViewElement<T> = apply {
    element.scrollerViewStyle.verticalScrollDisplay(ScrollDisplay.ALWAYS)
    element.scrollerViewStyle.horizontalScrollDisplay(ScrollDisplay.ALWAYS)
}

/**
 * Extension: Enable adaptive width
 */
fun <T : ScrollerView> ScrollerViewElement<T>.adaptiveWidth(enabled: Boolean = true): ScrollerViewElement<T> = apply {
    element.scrollerViewStyle.adaptiveWidth(enabled)
}

/**
 * Extension: Enable adaptive height
 */
fun <T : ScrollerView> ScrollerViewElement<T>.adaptiveHeight(enabled: Boolean = true): ScrollerViewElement<T> = apply {
    element.scrollerViewStyle.adaptiveHeight(enabled)
}
