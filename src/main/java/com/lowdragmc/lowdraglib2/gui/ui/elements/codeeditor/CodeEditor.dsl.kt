package com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor

import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.ILanguageDefinition
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.StyleManager
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextAreaElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextAreaSpec

/**
 * Specification for CodeEditor element (extends TextAreaSpec)
 */
open class CodeEditorSpec<T : CodeEditor>(
    var language: ILanguageDefinition? = null,
    var styleManager: StyleManager? = null,
) : TextAreaSpec<T>() {
    /**
     * Set syntax highlighting language
     */
    fun language(lang: ILanguageDefinition) = apply {
        this.language = lang
    }

    /**
     * Set JavaScript language
     */
    fun javascript() = apply {
        this.language = Languages.JAVASCRIPT
    }

    /**
     * Set LSS (CSS-like) language
     */
    fun lss() = apply {
        this.language = Languages.LSS
    }

    /**
     * Set XML language
     */
    fun xml() = apply {
        this.language = Languages.XML
    }

    /**
     * Set style manager
     */
    fun styleManager(manager: StyleManager) = apply {
        this.styleManager = manager
    }
}

/**
 * CodeEditor element builder (extends TextAreaElement)
 */
open class CodeEditorElement<T : CodeEditor>(
    element: T,
    spec: (CodeEditorSpec<T>.() -> Unit)? = null,
) : TextAreaElement<T>(element, null) {

    private val codeEditorSpec: CodeEditorSpec<T>?

    init {
        codeEditorSpec = spec?.let { CodeEditorSpec<T>().apply(it) }
    }

    override fun makeSpec(): TextAreaSpec<T>? {
        return codeEditorSpec
    }

    override fun build(spec: TextAreaSpec<T>?): T {
        val e = super.build(spec)
        if (spec is CodeEditorSpec<T>) {
            applyCodeEditorProperties(spec, e as CodeEditor)
        }
        return e
    }

    protected fun applyCodeEditorProperties(spec: CodeEditorSpec<T>, element: CodeEditor) {
        spec.language?.let { element.setLanguage(it) }
        spec.styleManager?.let { element.styleManager = it }
    }
}

/**
 * Top Level - Create a standalone CodeEditor element
 */
fun codeEditor(spec: (CodeEditorSpec<CodeEditor>.() -> Unit)? = null,
               init: CodeEditorElement<CodeEditor>.() -> Unit = {}): CodeEditor {
    return CodeEditorElement(CodeEditor(), spec).apply(init).build()
}

/**
 * Internal Builder - Add CodeEditor as a child to a container
 */
fun UIContainer<*, *>.codeEditor(spec: (CodeEditorSpec<CodeEditor>.() -> Unit)? = null,
                                  init: CodeEditorElement<CodeEditor>.() -> Unit = {}) =
    add(CodeEditorElement(CodeEditor(), spec), init)

/**
 * DSL converter - Convert existing CodeEditor to DSL builder
 */
fun <T : CodeEditor> T.dsl(spec: (CodeEditorSpec<T>.() -> Unit)? = null,
                           init: CodeEditorElement<T>.() -> Unit = {}): CodeEditorElement<T> {
    return CodeEditorElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set language
 */
fun <T : CodeEditor> CodeEditorElement<T>.withLanguage(language: ILanguageDefinition): CodeEditorElement<T> = apply {
    element.setLanguage(language)
}

/**
 * Extension: Set JavaScript language
 */
fun <T : CodeEditor> CodeEditorElement<T>.asJavaScript(): CodeEditorElement<T> = apply {
    element.setLanguage(Languages.JAVASCRIPT)
}

/**
 * Extension: Set LSS language
 */
fun <T : CodeEditor> CodeEditorElement<T>.asLSS(): CodeEditorElement<T> = apply {
    element.setLanguage(Languages.LSS)
}

/**
 * Extension: Set XML language
 */
fun <T : CodeEditor> CodeEditorElement<T>.asXML(): CodeEditorElement<T> = apply {
    element.setLanguage(Languages.XML)
}

/**
 * Extension: Set style manager
 */
fun <T : CodeEditor> CodeEditorElement<T>.withStyleManager(manager: StyleManager): CodeEditorElement<T> = apply {
    element.styleManager = manager
}

/**
 * Extension: Get current language
 */
fun <T : CodeEditor> CodeEditorElement<T>.getLanguage(): ILanguageDefinition {
    return element.language
}

/**
 * Extension: Get styled lines for custom rendering
 */
fun <T : CodeEditor> CodeEditorElement<T>.getStyledLines() = element.styledLines

/**
 * Extension: Access syntax parser
 */
fun <T : CodeEditor> CodeEditorElement<T>.syntaxParser() = element.syntaxParser

