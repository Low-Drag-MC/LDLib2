package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler
import com.lowdragmc.lowdraglib2.utils.search.ISearch
import java.util.function.Consumer

/**
 * Extension function for SearchComponent.SearchStyle DSL
 */
fun <T> SearchComponent<T>.searchStyleDsl(init: SearchComponent<T>.SearchStyle.() -> Unit = {}): SearchComponent<T> {
    this.searchStyle.apply(init)
    return this
}

/**
 * Specification for SearchComponent element
 */
open class SearchComponentSpec<T, E : SearchComponent<T>>(
    var searchStyle: (SearchComponent<T>.SearchStyle.() -> Unit)? = null,
    var searchUI: SearchComponent.ISearchUI<T>? = null,
    var candidateUIProvider: UIElementProvider<T>? = null,
    var selectedValue: T? = null,
    var onValueChanged: Consumer<T>? = null,
    var searchOnServer: Boolean? = null,
    var serverSearchClass: Class<Array<T>>? = null,
) : ElementSpec<E>() {

    // --- new: DSL-based searchUI builder ---
    var searchUIDsl: (SearchUiScope<T>.() -> Unit)? = null

    /**
     * Set search UI implementation (raw)
     */
    fun searchUI(ui: SearchComponent.ISearchUI<T>) = apply {
        this.searchUI = ui
        this.searchUIDsl = null
    }

    /**
     * Set search UI implementation (DSL)
     */
    fun searchUI(block: SearchUiScope<T>.() -> Unit) = apply {
        this.searchUIDsl = block
        this.searchUI = null
    }

    /**
     * Set candidate UI provider
     */
    fun candidateUI(provider: UIElementProvider<T>) = apply {
        this.candidateUIProvider = provider
    }

    /**
     * Set selected value
     */
    fun selected(value: T) = apply {
        this.selectedValue = value
    }

    /**
     * Set value change listener
     */
    fun onChange(handler: Consumer<T>) = apply {
        this.onValueChanged = handler
    }

    /**
     * Set value change listener (Kotlin lambda)
     */
    fun onChange(handler: (T) -> Unit) = apply {
        this.onValueChanged = Consumer { handler(it) }
    }

    /**
     * Enable server-side search
     */
    fun serverSearch(clazz: Class<Array<T>>) = apply {
        this.searchOnServer = true
        this.serverSearchClass = clazz
    }
}

/**
 * DSL scope to build a SearchComponent.ISearchUI<T>.
 *
 * Note: this describes SEARCH LOGIC (resultText/search/onSelected),
 * not the candidate UI rendering (use candidateUIProvider for that).
 */
class SearchUiScope<T> internal constructor() {
    internal var resultText: (T) -> String = { it.toString() }
    internal var onSelected: (T?) -> Unit = {}
    internal var search: ((word: String, find:IResultHandler<T>) -> Unit)? = null

    fun resultText(block: (T) -> String) {
        resultText = block
    }

    fun onSelected(block: (T?) -> Unit) {
        onSelected = block
    }

    /**
     * Implement the actual search.
     * Call [find] for each candidate you want to show.
     */
    fun search(block: (word: String, find: IResultHandler<T>) -> Unit) {
        search = block
    }

    internal fun build(): SearchComponent.ISearchUI<T> {
        val searchImpl = search ?: throw IllegalStateException("SearchComponentSpec.searchUI { } requires search { word, find -> ... }")
        val rt = resultText
        val sel = onSelected

        return object : SearchComponent.ISearchUI<T> {
            override fun resultText(value: T): String = rt(value)
            override fun onResultSelected(value: T?) = sel(value)
            override fun search(word: String, find: IResultHandler<T>) {
                searchImpl(word, find)
            }
        }
    }
}

/**
 * SearchComponent element builder
 */
open class SearchComponentElement<T, E : SearchComponent<T>>(
    element: E,
    spec: (SearchComponentSpec<T, E>.() -> Unit)? = null,
) : UIContainer<E, SearchComponentSpec<T, E>>(element, spec) {

    override fun makeSpec(): SearchComponentSpec<T, E>? {
        return spec?.let { SearchComponentSpec<T, E>().apply(it) }
    }

    override fun build(spec: SearchComponentSpec<T, E>?): E {
        val e = super.build(spec)

        spec?.searchStyle?.let { e.searchStyle(it) }
        spec?.candidateUIProvider?.let { e.setCandidateUIProvider(it) }

        spec?.searchUIDsl?.let { dsl ->
            val ui = SearchUiScope<T>().apply(dsl).build()
            e.setSearchUI(ui)
        } ?: spec?.searchUI?.let { ui ->
            e.setSearchUI(ui)
        }

        spec?.onValueChanged?.let { e.setOnValueChanged(it) }
        spec?.selectedValue?.let { e.setSelected(it) }

        spec?.searchOnServer?.let { onServer ->
            if (onServer) {
                val clazz = spec.serverSearchClass
                    ?: throw IllegalStateException("serverSearchClass is required when searchOnServer = true")
                @Suppress("UNCHECKED_CAST")
                e.setSearchOnServer(clazz as Class<Array<T>>)
            }
        }

        return e
    }
}

/**
 * Top Level - Create a standalone SearchComponent element
 */
fun <T> searchComponent(spec: (SearchComponentSpec<T, SearchComponent<T>>.() -> Unit)? = null,
                        init: SearchComponentElement<T, SearchComponent<T>>.() -> Unit = {}): SearchComponent<T> {
    return SearchComponentElement(SearchComponent<T>(), spec).apply(init).build()
}

/**
 * Internal Builder - Add SearchComponent as a child to a container
 */
fun <T> UIContainer<*, *>.searchComponent(spec: (SearchComponentSpec<T, SearchComponent<T>>.() -> Unit)? = null,
                                          init: SearchComponentElement<T, SearchComponent<T>>.() -> Unit = {}) =
    add(SearchComponentElement(SearchComponent<T>(), spec), init)

/**
 * DSL converter - Convert existing SearchComponent to DSL builder
 */
fun <T> SearchComponent<T>.dsl(spec: (SearchComponentSpec<T, SearchComponent<T>>.() -> Unit)? = null,
                               init: SearchComponentElement<T, SearchComponent<T>>.() -> Unit = {}): SearchComponentElement<T, SearchComponent<T>> {
    return SearchComponentElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set search UI
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.withSearchUI(ui: SearchComponent.ISearchUI<T>): SearchComponentElement<T, E> = apply {
    element.setSearchUI(ui)
}

/**
 * Extension: Set candidate UI provider
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.withCandidateUI(provider: UIElementProvider<T>): SearchComponentElement<T, E> = apply {
    element.setCandidateUIProvider(provider)
}

/**
 * Extension: Set selected value
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.withSelected(value: T): SearchComponentElement<T, E> = apply {
    element.setSelected(value)
}

/**
 * Extension: Set value change listener
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.onValueChange(handler: (T) -> Unit): SearchComponentElement<T, E> = apply {
    element.setOnValueChanged(Consumer { handler(it) })
}

/**
 * Extension: Show overlay on hover
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.withOverlay(): SearchComponentElement<T, E> = apply {
    element.searchStyle.showOverlay(true)
}

/**
 * Extension: Close after selection
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.closeOnSelect(): SearchComponentElement<T, E> = apply {
    element.searchStyle.closeAfterSelect(true)
}

/**
 * Extension: Set max item count before scrolling
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.withMaxItems(count: Int): SearchComponentElement<T, E> = apply {
    element.searchStyle.maxItemCount(count)
}

/**
 * Extension: Set scroller view height
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.withScrollHeight(height: Float): SearchComponentElement<T, E> = apply {
    element.searchStyle.scrollerViewHeight(height)
}

/**
 * Extension: Enable server-side search
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.enableServerSearch(clazz: Class<Array<T>>): SearchComponentElement<T, E> = apply {
    element.setSearchOnServer(clazz)
}

/**
 * Extension: Show search dialog
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.show(): SearchComponentElement<T, E> = apply {
    element.show()
}

/**
 * Extension: Hide search dialog
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.hide(): SearchComponentElement<T, E> = apply {
    element.hide()
}

/**
 * Extension: Configure text field
 */
fun <T, E : SearchComponent<T>> SearchComponentElement<T, E>.withTextField(config: TextField.() -> Unit): SearchComponentElement<T, E> = apply {
    element.textField.apply(config)
}
