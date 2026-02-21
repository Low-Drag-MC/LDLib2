package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import net.minecraft.network.chat.Component
import java.util.function.Consumer

/**
 * Specification for TabView element
 */
open class TabViewSpec<T : TabView>(
    var onTabSelected: Consumer<Tab>? = null,
) : ElementSpec<T>() {
    /**
     * Callback when a tab is selected (Kotlin lambda)
     */
    fun onTabSelected(handler: (Tab) -> Unit) = apply {
        this.onTabSelected = Consumer { handler(it) }
    }
}

/**
 * Represents a tab with its associated content for easy configuration
 */
data class TabWithContent(
    val tab: Tab,
    val content: UIElement
)

/**
 * TabView element builder
 */
open class TabViewElement<T : TabView>(
    element: T,
    spec: (TabViewSpec<T>.() -> Unit)? = null,
) : UIContainer<T, TabViewSpec<T>>(element, spec) {

    private val pendingTabs = mutableListOf<TabWithContent>()

    override fun makeSpec(): TabViewSpec<T>? {
        return spec?.let { TabViewSpec<T>().apply(it) }
    }

    override fun build(spec: TabViewSpec<T>?): T {
        val e = super.build(spec)
        applyTabViewProperties(spec, e)
        // Add pending tabs
        pendingTabs.forEach { e.addTab(it.tab, it.content) }
        return e
    }

    protected fun applyTabViewProperties(spec: TabViewSpec<T>?, element: TabView) {
        spec?.onTabSelected?.let { element.setOnTabSelected(it) }
    }

    /**
     * Add a tab with its content
     */
    fun tab(
        tabText: String,
        translate: Boolean = false,
        tabConfig: (Tab.() -> Unit)? = null,
        contentBuilder: UIElement.() -> Unit
    ) = apply {
        val tab = Tab().apply {
            setText(tabText, translate)
            tabConfig?.invoke(this)
        }
        val content = UIElement().apply(contentBuilder)
        pendingTabs.add(TabWithContent(tab, content))
    }

    /**
     * Add a tab with Component text and its content
     */
    fun tab(
        tabText: Component,
        tabConfig: (Tab.() -> Unit)? = null,
        contentBuilder: UIElement.() -> Unit
    ) = apply {
        val tab = Tab().apply {
            setText(tabText)
            tabConfig?.invoke(this)
        }
        val content = UIElement().apply(contentBuilder)
        pendingTabs.add(TabWithContent(tab, content))
    }

    /**
     * Add an existing Tab with content
     */
    fun tab(tab: Tab, content: UIElement) = apply {
        pendingTabs.add(TabWithContent(tab, content))
    }
}

/**
 * Top Level - Create a standalone TabView element
 */
fun tabView(spec: (TabViewSpec<TabView>.() -> Unit)? = null,
            init: TabViewElement<TabView>.() -> Unit = {}): TabView {
    return TabViewElement(TabView(), spec).apply(init).build()
}

/**
 * Internal Builder - Add TabView as a child to a container
 */
fun UIContainer<*, *>.tabView(spec: (TabViewSpec<TabView>.() -> Unit)? = null,
                               init: TabViewElement<TabView>.() -> Unit = {}) =
    add(TabViewElement(TabView(), spec), init)

/**
 * DSL converter - Convert existing TabView to DSL builder
 */
fun <T : TabView> T.dsl(spec: (TabViewSpec<T>.() -> Unit)? = null,
                        init: TabViewElement<T>.() -> Unit = {}): TabViewElement<T> {
    return TabViewElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Add a tab with simple text and content
 */
fun <T : TabView> TabViewElement<T>.addTab(
    text: String,
    translate: Boolean = false,
    content: UIElement.() -> Unit
): TabViewElement<T> = apply {
    val tab = Tab().setText(text, translate)
    val contentElement = UIElement().apply(content)
    element.addTab(tab, contentElement)
}

/**
 * Extension: Configure tab header container
 */
fun <T : TabView> TabViewElement<T>.withTabHeaderContainer(config: UIElement.() -> Unit): TabViewElement<T> = apply {
    element.tabHeaderContainer(Consumer { config(it) })
}

/**
 * Extension: Configure tab scroller
 */
fun <T : TabView> TabViewElement<T>.withTabScroller(config: ScrollerView.() -> Unit): TabViewElement<T> = apply {
    element.tabScroller(Consumer { config(it) })
}

/**
 * Extension: Configure tab content container
 */
fun <T : TabView> TabViewElement<T>.withTabContentContainer(config: UIElement.() -> Unit): TabViewElement<T> = apply {
    element.tabContentContainer(Consumer { config(it) })
}

/**
 * Extension: Set callback for tab selection
 */
fun <T : TabView> TabViewElement<T>.onTabSelected(handler: (Tab) -> Unit): TabViewElement<T> = apply {
    element.setOnTabSelected(Consumer { handler(it) })
}

/**
 * Extension: Select a specific tab
 */
fun <T : TabView> TabViewElement<T>.selectTab(tab: Tab): TabViewElement<T> = apply {
    element.selectTab(tab)
}

/**
 * Extension: Clear all tabs
 */
fun <T : TabView> TabViewElement<T>.clearTabs(): TabViewElement<T> = apply {
    element.clear()
}
