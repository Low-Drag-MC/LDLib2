package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import java.util.function.Consumer

/**
 * Extension function for TextArea.TextAreaStyle DSL
 */
fun <T : TextArea> T.textAreaStyleDsl(init: TextArea.TextAreaStyle.() -> Unit = {}): T {
    this.textAreaStyle.apply(init)
    return this
}

/**
 * Specification for TextArea element
 */
open class TextAreaSpec<T : TextArea>(
    var textAreaStyle: (TextArea.TextAreaStyle.() -> Unit)? = null,
    var lines: Array<String>? = null,
    var text: String? = null,
    var linesResponder: Consumer<Array<String>>? = null,
) : ElementSpec<T>() {
    /**
     * Set initial lines
     */
    fun lines(vararg lines: String) = apply {
        this.lines = arrayOf(*lines)
    }

    /**
     * Set initial text (split by newlines)
     */
    fun text(text: String) = apply {
        this.text = text
        this.lines = text.split("\n").toTypedArray()
    }

    /**
     * Set lines change listener
     */
    fun onLinesChanged(handler: Consumer<Array<String>>) = apply {
        this.linesResponder = handler
    }

    /**
     * Set lines change listener (Kotlin lambda)
     */
    fun onLinesChanged(handler: (Array<String>) -> Unit) = apply {
        this.linesResponder = Consumer { handler(it) }
    }
}

/**
 * TextArea element builder
 */
open class TextAreaElement<T : TextArea>(
    element: T,
    spec: (TextAreaSpec<T>.() -> Unit)? = null,
) : UIContainer<T, TextAreaSpec<T>>(element, spec) {
    override fun makeSpec(): TextAreaSpec<T>? {
        return spec?.let { TextAreaSpec<T>().apply(it) }
    }

    override fun build(spec: TextAreaSpec<T>?): T {
        val e = super.build(spec)
        applyTextAreaProperties(spec, e)
        return e
    }

    protected fun applyTextAreaProperties(spec: TextAreaSpec<T>?, element: TextArea) {
        spec?.textAreaStyle?.let(element.textAreaStyle::apply)
        spec?.lines?.let { element.setLines(it, false) }
        spec?.linesResponder?.let { element.setLinesResponder(it) }
    }
}

/**
 * Top Level - Create a standalone TextArea element
 */
fun textArea(spec: (TextAreaSpec<TextArea>.() -> Unit)? = null,
             init: TextAreaElement<TextArea>.() -> Unit = {}): TextArea {
    return TextAreaElement(TextArea(), spec).apply(init).build()
}

/**
 * Internal Builder - Add TextArea as a child to a container
 */
fun UIContainer<*, *>.textArea(spec: (TextAreaSpec<TextArea>.() -> Unit)? = null,
                                init: TextAreaElement<TextArea>.() -> Unit = {}) =
    add(TextAreaElement(TextArea(), spec), init)

/**
 * DSL converter - Convert existing TextArea to DSL builder
 */
fun <T : TextArea> T.dsl(spec: (TextAreaSpec<T>.() -> Unit)? = null,
                         init: TextAreaElement<T>.() -> Unit = {}): TextAreaElement<T> {
    return TextAreaElement(this, spec).apply(init)
}
