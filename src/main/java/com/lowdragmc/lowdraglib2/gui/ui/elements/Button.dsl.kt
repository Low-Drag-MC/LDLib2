package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent
import net.minecraft.network.chat.Component

fun <T : Button> T.buttonStyleDsl(init: Button.ButtonStyle.() -> Unit = {}): T {
    this.buttonStyle.apply(init)
    return this
}

open class ButtonSpec<T : Button>(
    var buttonStyle: (Button.ButtonStyle.() -> Unit)? = null,
    var text: Component? = null,
    var onClick: ((UIEvent) -> Unit)? = null,
    var onServerClick: ((UIEvent) -> Unit)? = null,
) : ElementSpec<T>() {
    fun text(text: String, translate: Boolean = true) = apply {
        this.text = Component.translatable(text, translate)
    }
    fun noText() = apply { this.text = Component.empty() }
}

open class ButtonElement<T : Button>(
    element: T,
    spec: (ButtonSpec<T>.() -> Unit)? = null,
) : UIContainer<T, ButtonSpec<T>>(element, spec) {
    override fun makeSpec(): ButtonSpec<T>? {
        return spec?.let { ButtonSpec<T>().apply(it) }
    }

    override fun build(spec: ButtonSpec<T>?): T {
        val e = super.build(spec)
        applyButtonStyle(spec, e)
        return e
    }

    protected fun applyButtonStyle(spec:ButtonSpec<T>?, element: Button) {
        spec?.buttonStyle?.let(element.buttonStyle::apply)
        spec?.text?.let{ if(it == Component.empty()) element.noText() else element.setText(it) }
        spec?.onClick?.let{ element.setOnClick(it) }
        spec?.onServerClick?.let{ element.setOnServerClick(it) }
    }
}

/**
 * Top Level
 */
fun button(spec: (ButtonSpec<Button>.() -> Unit)? = null,
           init: ButtonElement<Button>.() -> Unit): Button {
    return ButtonElement(Button(), spec).apply(init).build()
}

/**
 * Internal Builder
 */
fun UIContainer<*, *>.button(spec: (ButtonSpec<Button>.() -> Unit)? = null,
                             init: ButtonElement<Button>.() -> Unit = {}) =
    add(ButtonElement(Button(), spec), init)

/**
 * Dsl converter
 */
fun <T : Button> T.dsl(spec: (ButtonSpec<T>.() -> Unit)? = null, init: ButtonElement<T>.() -> Unit = {}): ButtonElement<T> {
    return ButtonElement(this, spec).apply(init)
}