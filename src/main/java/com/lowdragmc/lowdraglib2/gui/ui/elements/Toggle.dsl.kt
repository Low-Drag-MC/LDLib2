package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import net.minecraft.network.chat.Component

/**
 * Extension function for Toggle.ToggleStyle DSL
 */
fun <T : Toggle> T.toggleStyleDsl(init: Toggle.ToggleStyle.() -> Unit = {}): T {
    this.toggleStyle.apply(init)
    return this
}

/**
 * Specification for Toggle element
 */
open class ToggleSpec<T : Toggle>(
    var toggleStyle: (Toggle.ToggleStyle.() -> Unit)? = null,
    var text: Component? = null,
    var isOn: Boolean? = null,
    var onToggleChanged: ((Boolean) -> Unit)? = null,
    var toggleGroup: Toggle.ToggleGroup? = null,
) : ElementSpec<T>() {
    fun text(text: String, translate: Boolean = true) = apply {
        this.text = if (translate) Component.translatable(text) else Component.literal(text)
    }

    fun noText() = apply {
        this.text = Component.empty()
    }

    /**
     * Convenience alias for onToggleChanged
     */
    fun onToggle(handler: (Boolean) -> Unit) = apply {
        this.onToggleChanged = handler
    }
}

/**
 * Toggle element builder
 */
open class ToggleElement<T : Toggle>(
    element: T,
    spec: (ToggleSpec<T>.() -> Unit)? = null,
) : UIContainer<T, ToggleSpec<T>>(element, spec) {
    override fun makeSpec(): ToggleSpec<T>? {
        return spec?.let { ToggleSpec<T>().apply(it) }
    }

    override fun build(spec: ToggleSpec<T>?): T {
        val e = super.build(spec)
        applyToggleProperties(spec, e)
        return e
    }

    protected fun applyToggleProperties(spec: ToggleSpec<T>?, element: Toggle) {
        spec?.toggleStyle?.let(element.toggleStyle::apply)
        spec?.text?.let {
            if (it == Component.empty()) element.noText() else element.setText(it)
        }
        spec?.isOn?.let { element.setOn(it) }
        spec?.onToggleChanged?.let { handler -> element.setOnToggleChanged { handler(it) } }
        spec?.toggleGroup?.let(element::setToggleGroup)
    }
}

/**
 * Top Level - Create a standalone Toggle element
 */
fun toggle(spec: (ToggleSpec<Toggle>.() -> Unit)? = null,
           init: ToggleElement<Toggle>.() -> Unit = {}): Toggle {
    return ToggleElement(Toggle(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Toggle as a child to a container
 */
fun UIContainer<*, *>.toggle(spec: (ToggleSpec<Toggle>.() -> Unit)? = null,
                              init: ToggleElement<Toggle>.() -> Unit = {}) =
    add(ToggleElement(Toggle(), spec), init)

/**
 * DSL converter - Convert existing Toggle to DSL builder
 */
fun <T : Toggle> T.dsl(spec: (ToggleSpec<T>.() -> Unit)? = null,
                       init: ToggleElement<T>.() -> Unit = {}): ToggleElement<T> {
    return ToggleElement(this, spec).apply(init)
}
