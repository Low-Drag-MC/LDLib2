package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import net.minecraft.network.chat.Component

fun <T : TextElement> T.textStyleDsl(init: TextElement.TextStyle.() -> Unit = {}): T {
    this.textStyle.apply(init)
    return this
}

open class TextSpec<T : TextElement>(
    var textStyle: (TextElement.TextStyle.() -> Unit)? = null,
    var text: Component? = null,
) : ElementSpec<T>() {
    fun text(text: String, translate: Boolean = true) = apply {
        this.text = Component.translatable(text, translate)
    }
    fun noText() = apply { this.text = Component.empty() }
}

open class TextEleElement<T : TextElement>(
    element: T,
    spec: (TextSpec<T>.() -> Unit)? = null,
) : UIContainer<T, TextSpec<T>>(element, spec) {
    override fun makeSpec(): TextSpec<T>? {
        return spec?.let { TextSpec<T>().apply(it) }
    }

    override fun build(spec: TextSpec<T>?): T {
        val e = super.build(spec)
        applyTextStyle(spec, e)
        return e
    }

    protected fun applyTextStyle(spec:TextSpec<T>?, element: TextElement) {
        spec?.textStyle?.let(element.textStyle::apply)
        spec?.text?.let(element::setText)
    }
}

/**
 * Top Level
 */
fun text(spec: (TextSpec<TextElement>.() -> Unit)? = null,
           init: TextEleElement<TextElement>.() -> Unit): TextElement {
    return TextEleElement(TextElement(), spec).apply(init).build()
}

fun label(spec: (TextSpec<Label>.() -> Unit)? = null,
          init: TextEleElement<Label>.() -> Unit): Label {
    return TextEleElement(Label(), spec).apply(init).build()
}

/**
 * Internal Builder
 */
fun UIContainer<*, *>.text(spec: (TextSpec<TextElement>.() -> Unit)? = null,
                             init: TextEleElement<TextElement>.() -> Unit = {}) =
    add(TextEleElement(TextElement(), spec), init)

fun UIContainer<*, *>.label(spec: (TextSpec<Label>.() -> Unit)? = null,
                           init: TextEleElement<Label>.() -> Unit = {}) =
    add(TextEleElement(Label(), spec), init)

/**
 * Dsl converter
 */
fun <T : TextElement> T.dsl(spec: (TextSpec<T>.() -> Unit)? = null, init: TextEleElement<T>.() -> Unit = {}): TextEleElement<T> {
    return TextEleElement(this, spec).apply(init)
}

