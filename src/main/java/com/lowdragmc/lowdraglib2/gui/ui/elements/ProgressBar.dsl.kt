package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import net.minecraft.network.chat.Component

/**
 * Extension function for ProgressBar.ProgressBarStyle DSL
 */
fun <T : ProgressBar> T.progressBarStyleDsl(init: ProgressBar.ProgressBarStyle.() -> Unit = {}): T {
    this.progressBarStyle.apply(init)
    return this
}

/**
 * Specification for ProgressBar element
 */
open class ProgressBarSpec<T : ProgressBar>(
    var progressBarStyle: (ProgressBar.ProgressBarStyle.() -> Unit)? = null,
    var minValue: Float? = null,
    var maxValue: Float? = null,
    var value: Float? = null,
    var labelText: Component? = null,
) : ElementSpec<T>() {
    /**
     * Set the range of the progress bar
     */
    fun range(min: Float, max: Float) = apply {
        this.minValue = min
        this.maxValue = max
    }

    /**
     * Set label text with translation
     */
    fun label(text: String, translate: Boolean = true) = apply {
        this.labelText = if (translate) Component.translatable(text) else Component.literal(text)
    }

    /**
     * Set label component
     */
    fun label(text: Component) = apply {
        this.labelText = text
    }

}

/**
 * ProgressBar element builder
 */
open class ProgressBarElement<T : ProgressBar>(
    element: T,
    spec: (ProgressBarSpec<T>.() -> Unit)? = null,
) : UIContainer<T, ProgressBarSpec<T>>(element, spec) {
    override fun makeSpec(): ProgressBarSpec<T>? {
        return spec?.let { ProgressBarSpec<T>().apply(it) }
    }

    override fun build(spec: ProgressBarSpec<T>?): T {
        val e = super.build(spec)
        applyProgressBarProperties(spec, e)
        return e
    }

    protected fun applyProgressBarProperties(spec: ProgressBarSpec<T>?, element: ProgressBar) {
        spec?.progressBarStyle?.let(element.progressBarStyle::apply)

        // Apply range first if both min and max are specified
        if (spec?.minValue != null || spec?.maxValue != null) {
            val min = spec.minValue ?: element.minValue
            val max = spec.maxValue ?: element.maxValue
            element.setRange(min, max)
        }

        spec?.value?.let { element.setProgress(it) }
        spec?.labelText?.let { element.label.setText(it) }
    }
}

/**
 * Top Level - Create a standalone ProgressBar element
 */
fun progressBar(spec: (ProgressBarSpec<ProgressBar>.() -> Unit)? = null,
                init: ProgressBarElement<ProgressBar>.() -> Unit = {}): ProgressBar {
    return ProgressBarElement(ProgressBar(), spec).apply(init).build()
}

/**
 * Internal Builder - Add ProgressBar as a child to a container
 */
fun UIContainer<*, *>.progressBar(spec: (ProgressBarSpec<ProgressBar>.() -> Unit)? = null,
                                   init: ProgressBarElement<ProgressBar>.() -> Unit = {}) =
    add(ProgressBarElement(ProgressBar(), spec), init)

/**
 * DSL converter - Convert existing ProgressBar to DSL builder
 */
fun <T : ProgressBar> T.dsl(spec: (ProgressBarSpec<T>.() -> Unit)? = null,
                            init: ProgressBarElement<T>.() -> Unit = {}): ProgressBarElement<T> {
    return ProgressBarElement(this, spec).apply(init)
}

