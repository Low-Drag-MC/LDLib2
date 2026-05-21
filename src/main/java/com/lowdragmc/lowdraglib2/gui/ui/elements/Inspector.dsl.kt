package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.configurator.IConfigurable
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator
import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack
import java.util.function.Consumer

/**
 * Specification for Inspector element
 */
open class InspectorSpec<T : Inspector>(
    var historyStack: IHistoryStack? = null,
    var initialConfigurable: IConfigurable? = null,
    var configuratorListener: Consumer<Configurator>? = null,
    var onClose: Runnable? = null,
    var historyAction: Runnable? = null,
) : ElementSpec<T>() {
    /**
     * Set history stack for undo/redo
     */
    fun history(stack: IHistoryStack) = apply {
        this.historyStack = stack
    }

    /**
     * Set initial configurable to inspect
     */
    fun inspect(configurable: IConfigurable) = apply {
        this.initialConfigurable = configurable
    }

    /**
     * Set change listener for configurators
     */
    fun onChange(listener: Consumer<Configurator>) = apply {
        this.configuratorListener = listener
    }

    /**
     * Set change listener (Kotlin lambda)
     */
    fun onChange(listener: (Configurator) -> Unit) = apply {
        this.configuratorListener = Consumer { listener(it) }
    }

    /**
     * Set close callback
     */
    fun onClose(callback: Runnable) = apply {
        this.onClose = callback
    }

    /**
     * Set close callback (Kotlin lambda)
     */
    fun onClose(callback: () -> Unit) = apply {
        this.onClose = Runnable { callback() }
    }

    /**
     * Set history action callback
     */
    fun onHistoryAction(action: Runnable) = apply {
        this.historyAction = action
    }

    /**
     * Set history action callback (Kotlin lambda)
     */
    fun onHistoryAction(action: () -> Unit) = apply {
        this.historyAction = Runnable { action() }
    }
}

/**
 * Inspector element builder
 */
open class InspectorElement<T : Inspector>(
    element: T,
    spec: (InspectorSpec<T>.() -> Unit)? = null,
) : UIContainer<T, InspectorSpec<T>>(element, spec) {
    override fun makeSpec(): InspectorSpec<T>? {
        return spec?.let { InspectorSpec<T>().apply(it) }
    }

    override fun build(spec: InspectorSpec<T>?): T {
        val e = super.build(spec)
        applyInspectorProperties(spec, e)
        return e
    }

    @Suppress("UNCHECKED_CAST")
    protected fun applyInspectorProperties(spec: InspectorSpec<T>?, element: Inspector) {
        spec?.historyStack?.let { element.historyStack = it }

        // Inspect initial configurable if provided
        if (spec?.initialConfigurable != null) {
            element.inspect(
                spec.initialConfigurable!!,
                spec.configuratorListener,
                spec.onClose,
                spec.historyAction
            )
        }
    }
}

/**
 * Top Level - Create a standalone Inspector element
 */
fun inspector(spec: (InspectorSpec<Inspector>.() -> Unit)? = null,
              init: InspectorElement<Inspector>.() -> Unit = {}): Inspector {
    return InspectorElement(Inspector(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Inspector as a child to a container
 */
fun UIContainer<*, *>.inspector(spec: (InspectorSpec<Inspector>.() -> Unit)? = null,
                                 init: InspectorElement<Inspector>.() -> Unit = {}) =
    add(InspectorElement(Inspector(), spec), init)

/**
 * DSL converter - Convert existing Inspector to DSL builder
 */
fun <T : Inspector> T.dsl(spec: (InspectorSpec<T>.() -> Unit)? = null,
                          init: InspectorElement<T>.() -> Unit = {}): InspectorElement<T> {
    return InspectorElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set history stack
 */
fun <T : Inspector> InspectorElement<T>.withHistory(stack: IHistoryStack): InspectorElement<T> = apply {
    element.historyStack = stack
}

/**
 * Extension: Inspect a configurable
 */
fun <T : Inspector> InspectorElement<T>.inspect(configurable: IConfigurable): InspectorElement<T> = apply {
    element.inspect(configurable)
}

/**
 * Extension: Inspect with listener
 */
fun <T : Inspector> InspectorElement<T>.inspect(
    configurable: IConfigurable,
    listener: (Configurator) -> Unit
): InspectorElement<T> = apply {
    element.inspect(configurable, Consumer { listener(it) })
}

/**
 * Extension: Inspect with full configuration
 */
fun <T : Inspector, C : IConfigurable> InspectorElement<T>.inspect(
    configurable: C,
    listener: ((Configurator) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    historyAction: (() -> Unit)? = null
): InspectorElement<T> = apply {
    element.inspect(
        configurable,
        listener?.let { Consumer { listener(it) } },
        onClose?.let { Runnable { it() } },
        historyAction?.let { Runnable { historyAction() } }
    )
}

/**
 * Extension: Clear inspection
 */
fun <T : Inspector> InspectorElement<T>.clear(): InspectorElement<T> = apply {
    element.clear()
}

/**
 * Extension: Configure scroller view
 */
fun <T : Inspector> InspectorElement<T>.withScrollerView(config: ScrollerView.() -> Unit): InspectorElement<T> = apply {
    element.scrollerView.apply(config)
}

/**
 * Extension: Get current inspected configurable
 */
fun <T : Inspector> InspectorElement<T>.getInspected(): IConfigurable? {
    return element.inspectedConfigurable
}

