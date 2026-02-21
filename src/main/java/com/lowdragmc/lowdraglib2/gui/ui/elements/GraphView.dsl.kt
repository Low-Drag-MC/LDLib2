package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIBuilder
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement

/**
 * Extension function for GraphView.GraphViewStyle DSL
 */
fun <T : GraphView> T.graphViewStyleDsl(init: GraphView.GraphViewStyle.() -> Unit = {}): T {
    this.graphViewStyle(init)
    return this
}

/**
 * Specification for GraphView element
 */
open class GraphViewSpec<T : GraphView>(
    var graphViewStyle: (GraphView.GraphViewStyle.() -> Unit)? = null,
) : ElementSpec<T>()

/**
 * GraphView element builder
 */
open class GraphViewElement<T : GraphView>(
    element: T,
    spec: (GraphViewSpec<T>.() -> Unit)? = null,
) : UIContainer<T, GraphViewSpec<T>>(element, spec) {
    override fun makeSpec(): GraphViewSpec<T>? {
        return spec?.let { GraphViewSpec<T>().apply(it) }
    }

    override fun build(spec: GraphViewSpec<T>?): T {
        val e = super.build(spec)
        applyGraphViewProperties(spec, e)
        return e
    }

    protected fun applyGraphViewProperties(spec: GraphViewSpec<T>?, element: GraphView) {
        spec?.graphViewStyle?.let { element.graphViewStyle(it) }
    }

    /**
     * Override addChild to add to content root instead of directly
     */
    override fun addChild(child: UIBuilder<*>) {
        element.addContentChild(child.build())
    }

    /**
     * Add a child element to the graph content
     */
    fun content(child: UIElement): GraphViewElement<T> = apply {
        element.addContentChild(child)
    }

    /**
     * Configure content root
     */
    fun contentRoot(config: UIElement.() -> Unit): GraphViewElement<T> = apply {
        element.contentRoot.apply(config)
    }
}

/**
 * Top Level - Create a standalone GraphView element
 */
fun graphView(spec: (GraphViewSpec<GraphView>.() -> Unit)? = null,
              init: GraphViewElement<GraphView>.() -> Unit = {}): GraphView {
    return GraphViewElement(GraphView(), spec).apply(init).build()
}

/**
 * Internal Builder - Add GraphView as a child to a container
 */
fun UIContainer<*, *>.graphView(spec: (GraphViewSpec<GraphView>.() -> Unit)? = null,
                                 init: GraphViewElement<GraphView>.() -> Unit = {}) =
    add(GraphViewElement(GraphView(), spec), init)

/**
 * DSL converter - Convert existing GraphView to DSL builder
 */
fun <T : GraphView> T.dsl(spec: (GraphViewSpec<T>.() -> Unit)? = null,
                          init: GraphViewElement<T>.() -> Unit = {}): GraphViewElement<T> {
    return GraphViewElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Add content child
 */
fun <T : GraphView> GraphViewElement<T>.addContent(child: UIElement): GraphViewElement<T> = apply {
    element.addContentChild(child)
}

/**
 * Extension: Remove content child
 */
fun <T : GraphView> GraphViewElement<T>.removeContent(child: UIElement): GraphViewElement<T> = apply {
    element.removeContentChild(child)
}

/**
 * Extension: Clear all content
 */
fun <T : GraphView> GraphViewElement<T>.clearContent(): GraphViewElement<T> = apply {
    element.clearAllContentChildren()
}

/**
 * Extension: Set offset
 */
fun <T : GraphView> GraphViewElement<T>.withOffset(x: Float, y: Float): GraphViewElement<T> = apply {
    element.offsetX = x
    element.offsetY = y
}

/**
 * Extension: Enable zoom
 */
fun <T : GraphView> GraphViewElement<T>.enableZoom(enable: Boolean = true): GraphViewElement<T> = apply {
    element.graphViewStyle.allowZoom(enable)
}

/**
 * Extension: Enable pan
 */
fun <T : GraphView> GraphViewElement<T>.enablePan(enable: Boolean = true): GraphViewElement<T> = apply {
    element.graphViewStyle.allowPan(enable)
}

/**
 * Extension: Set scale range
 */
fun <T : GraphView> GraphViewElement<T>.scaleRange(min: Float, max: Float): GraphViewElement<T> = apply {
    element.graphViewStyle.minScale(min)
    element.graphViewStyle.maxScale(max)
}

/**
 * Extension: Set grid
 */
fun <T : GraphView> GraphViewElement<T>.withGrid(texture: IGuiTexture, size: Float): GraphViewElement<T> = apply {
    element.graphViewStyle.gridTexture(texture)
    element.graphViewStyle.gridSize(size)
}

/**
 * Extension: Fit view to show all children
 */
fun <T : GraphView> GraphViewElement<T>.fitToChildren(padding: Float = 20f, minScale: Float = 0.1f): GraphViewElement<T> = apply {
    element.fitToChildren(padding, minScale)
}

/**
 * Extension: Fit view to specific bounds
 */
fun <T : GraphView> GraphViewElement<T>.fitToBounds(minX: Float, minY: Float, maxX: Float, maxY: Float, minScale: Float = 0.1f): GraphViewElement<T> = apply {
    element.fit(minX, minY, maxX, maxY, minScale)
}

/**
 * Extension: Configure content root
 */
fun <T : GraphView> GraphViewElement<T>.withContentRoot(config: UIElement.() -> Unit): GraphViewElement<T> = apply {
    element.contentRoot.apply(config)
}

