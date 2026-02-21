package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer

/**
 * Specification for ToggleGroupElement
 */
open class ToggleGroupElementSpec<T : ToggleGroupElement>(
    var allowEmpty: Boolean? = null,
) : ElementSpec<T>() {
    /**
     * Allow no toggles to be selected
     */
    fun allowEmpty() = apply {
        this.allowEmpty = true
    }

    /**
     * Require at least one toggle to be selected
     */
    fun requireSelection() = apply {
        this.allowEmpty = false
    }
}

/**
 * ToggleGroupElement builder
 */
open class ToggleGroupElementElement<T : ToggleGroupElement>(
    element: T,
    spec: (ToggleGroupElementSpec<T>.() -> Unit)? = null,
) : UIContainer<T, ToggleGroupElementSpec<T>>(element, spec) {
    override fun makeSpec(): ToggleGroupElementSpec<T>? {
        return spec?.let { ToggleGroupElementSpec<T>().apply(it) }
    }

    override fun build(spec: ToggleGroupElementSpec<T>?): T {
        val e = super.build(spec)
        applyToggleGroupElementProperties(spec, e)
        return e
    }

    protected fun applyToggleGroupElementProperties(spec: ToggleGroupElementSpec<T>?, element: ToggleGroupElement) {
        spec?.allowEmpty?.let { element.toggleGroup.setAllowEmpty(it) }
    }
}

/**
 * Top Level - Create a standalone ToggleGroupElement
 */
fun toggleGroup(spec: (ToggleGroupElementSpec<ToggleGroupElement>.() -> Unit)? = null,
                init: ToggleGroupElementElement<ToggleGroupElement>.() -> Unit = {}): ToggleGroupElement {
    return ToggleGroupElementElement(ToggleGroupElement(), spec).apply(init).build()
}

/**
 * Internal Builder - Add ToggleGroupElement as a child to a container
 */
fun UIContainer<*, *>.toggleGroup(spec: (ToggleGroupElementSpec<ToggleGroupElement>.() -> Unit)? = null,
                                   init: ToggleGroupElementElement<ToggleGroupElement>.() -> Unit = {}) =
    add(ToggleGroupElementElement(ToggleGroupElement(), spec), init)

/**
 * DSL converter - Convert existing ToggleGroupElement to DSL builder
 */
fun <T : ToggleGroupElement> T.dsl(spec: (ToggleGroupElementSpec<T>.() -> Unit)? = null,
                                   init: ToggleGroupElementElement<T>.() -> Unit = {}): ToggleGroupElementElement<T> {
    return ToggleGroupElementElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Allow empty selection
 */
fun <T : ToggleGroupElement> ToggleGroupElementElement<T>.allowEmpty(allow: Boolean = true): ToggleGroupElementElement<T> = apply {
    element.toggleGroup.setAllowEmpty(allow)
}

/**
 * Extension: Require at least one toggle selected
 */
fun <T : ToggleGroupElement> ToggleGroupElementElement<T>.requireSelection(): ToggleGroupElementElement<T> = apply {
    element.toggleGroup.setAllowEmpty(false)
}

/**
 * Extension: Get the current selected toggle
 */
fun <T : ToggleGroupElement> ToggleGroupElementElement<T>.getSelectedToggle(): Toggle? {
    return element.toggleGroup.currentToggle
}

/**
 * Extension: Get all toggles in the group
 */
fun <T : ToggleGroupElement> ToggleGroupElementElement<T>.getAllToggles(): List<Toggle> {
    return element.toggleGroup.toggles
}
