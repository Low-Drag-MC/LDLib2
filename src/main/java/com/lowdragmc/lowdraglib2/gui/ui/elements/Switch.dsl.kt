package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer

/**
 * Extension function for Switch.SwitchStyle DSL
 */
fun <T : Switch> T.switchStyleDsl(init: Switch.SwitchStyle.() -> Unit = {}): T {
    this.switchStyle.apply(init)
    return this
}

/**
 * Specification for Switch element
 */
open class SwitchSpec<T : Switch>(
    var switchStyle: (Switch.SwitchStyle.() -> Unit)? = null,
    var isOn: Boolean? = null,
    var onSwitchChanged: ((Boolean) -> Unit)? = null,
) : ElementSpec<T>() {
    /**
     * Convenience alias for onSwitchChanged
     */
    fun onSwitch(handler: (Boolean) -> Unit) = apply {
        this.onSwitchChanged = handler
    }
}

/**
 * Switch element builder
 */
open class SwitchElement<T : Switch>(
    element: T,
    spec: (SwitchSpec<T>.() -> Unit)? = null,
) : UIContainer<T, SwitchSpec<T>>(element, spec) {
    override fun makeSpec(): SwitchSpec<T>? {
        return spec?.let { SwitchSpec<T>().apply(it) }
    }

    override fun build(spec: SwitchSpec<T>?): T {
        val e = super.build(spec)
        applySwitchProperties(spec, e)
        return e
    }

    protected fun applySwitchProperties(spec: SwitchSpec<T>?, element: Switch) {
        spec?.switchStyle?.let(element.switchStyle::apply)
        spec?.isOn?.let { element.setOn(it) }
        spec?.onSwitchChanged?.let { handler -> element.setOnSwitchChanged { handler(it) } }
    }
}

/**
 * Top Level - Create a standalone Switch element
 */
fun switch(spec: (SwitchSpec<Switch>.() -> Unit)? = null,
           init: SwitchElement<Switch>.() -> Unit = {}): Switch {
    return SwitchElement(Switch(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Switch as a child to a container
 */
fun UIContainer<*, *>.switch(spec: (SwitchSpec<Switch>.() -> Unit)? = null,
                              init: SwitchElement<Switch>.() -> Unit = {}) =
    add(SwitchElement(Switch(), spec), init)

/**
 * DSL converter - Convert existing Switch to DSL builder
 */
fun <T : Switch> T.dsl(spec: (SwitchSpec<T>.() -> Unit)? = null,
                       init: SwitchElement<T>.() -> Unit = {}): SwitchElement<T> {
    return SwitchElement(this, spec).apply(init)
}
