package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import net.minecraft.network.chat.Component
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Extension function for Tab.TabStyle DSL
 */
fun <T : Tab> T.tabStyleDsl(init: Tab.TabStyle.() -> Unit = {}): T {
    this.tabStyle.apply(init)
    return this
}

/**
 * Specification for Tab element
 */
open class TabSpec<T : Tab>(
    var tabStyle: (Tab.TabStyle.() -> Unit)? = null,
    var text: Component? = null,
    var dynamicText: Supplier<Component>? = null,
    var textStyle: (TextElement.TextStyle.() -> Unit)? = null,
    var selected: Boolean? = null,
    var onTabSelected: Runnable? = null,
    var onTabUnselected: Runnable? = null,
) : ElementSpec<T>() {
    /**
     * Set static text with translation
     */
    fun text(text: String, translate: Boolean = false) = apply {
        this.text = if (translate) Component.translatable(text) else Component.literal(text)
    }

    /**
     * Set static text component
     */
    fun text(text: Component) = apply {
        this.text = text
    }

    /**
     * Set dynamic text from supplier
     */
    fun dynamicText(supplier: Supplier<Component>) = apply {
        this.dynamicText = supplier
    }

    /**
     * Set dynamic text from lambda
     */
    fun dynamicText(supplier: () -> Component) = apply {
        this.dynamicText = Supplier { supplier() }
    }

    /**
     * Callback when tab is selected
     */
    fun onSelected(callback: Runnable) = apply {
        this.onTabSelected = callback
    }

    /**
     * Callback when tab is selected (Kotlin lambda)
     */
    fun onSelected(callback: () -> Unit) = apply {
        this.onTabSelected = Runnable { callback() }
    }

    /**
     * Callback when tab is unselected
     */
    fun onUnselected(callback: Runnable) = apply {
        this.onTabUnselected = callback
    }

    /**
     * Callback when tab is unselected (Kotlin lambda)
     */
    fun onUnselected(callback: () -> Unit) = apply {
        this.onTabUnselected = Runnable { callback() }
    }
}

/**
 * Tab element builder
 */
open class TabElement<T : Tab>(
    element: T,
    spec: (TabSpec<T>.() -> Unit)? = null,
) : UIContainer<T, TabSpec<T>>(element, spec) {
    override fun makeSpec(): TabSpec<T>? {
        return spec?.let { TabSpec<T>().apply(it) }
    }

    override fun build(spec: TabSpec<T>?): T {
        val e = super.build(spec)
        applyTabProperties(spec, e)
        return e
    }

    protected fun applyTabProperties(spec: TabSpec<T>?, element: Tab) {
        spec?.tabStyle?.let(element.tabStyle::apply)

        // Handle dynamic text first (takes precedence over static text)
        if (spec?.dynamicText != null) {
            spec.dynamicText?.let { element.setDynamicText(it) }
        } else {
            spec?.text?.let { element.setText(it) }
        }

        spec?.selected?.let { element.setSelected(it) }
        spec?.onTabSelected?.let { element.setOnTabSelected(it) }
        spec?.onTabUnselected?.let { element.setOnTabUnselected(it) }
    }
}

/**
 * Top Level - Create a standalone Tab element
 */
fun tab(spec: (TabSpec<Tab>.() -> Unit)? = null,
        init: TabElement<Tab>.() -> Unit = {}): Tab {
    return TabElement(Tab(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Tab as a child to a container
 */
fun UIContainer<*, *>.tab(spec: (TabSpec<Tab>.() -> Unit)? = null,
                          init: TabElement<Tab>.() -> Unit = {}) =
    add(TabElement(Tab(), spec), init)

/**
 * DSL converter - Convert existing Tab to DSL builder
 */
fun <T : Tab> T.dsl(spec: (TabSpec<T>.() -> Unit)? = null,
                    init: TabElement<T>.() -> Unit = {}): TabElement<T> {
    return TabElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set text with translation
 */
fun <T : Tab> TabElement<T>.withText(text: String, translate: Boolean = false): TabElement<T> = apply {
    element.setText(text, translate)
}

/**
 * Extension: Set dynamic text
 */
fun <T : Tab> TabElement<T>.withDynamicText(supplier: () -> Component): TabElement<T> = apply {
    element.setDynamicText(Supplier { supplier() })
}

/**
 * Extension: Configure text style
 */
fun <T : Tab> TabElement<T>.withTextStyle(config: TextElement.TextStyle.() -> Unit): TabElement<T> = apply {
    element.textStyle(Consumer { config(it) })
}

/**
 * Extension: Set selected state
 */
fun <T : Tab> TabElement<T>.asSelected(selected: Boolean = true): TabElement<T> = apply {
    element.setSelected(selected)
}

/**
 * Extension: Add selection callback
 */
fun <T : Tab> TabElement<T>.onSelect(callback: () -> Unit): TabElement<T> = apply {
    element.setOnTabSelected(Runnable { callback() })
}

/**
 * Extension: Add unselection callback
 */
fun <T : Tab> TabElement<T>.onUnselect(callback: () -> Unit): TabElement<T> = apply {
    element.setOnTabUnselected(Runnable { callback() })
}
