package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import net.minecraft.network.chat.Component
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * Extension function for TextField.TextFieldStyle DSL
 */
fun <T : TextField> T.textFieldStyleDsl(init: TextField.TextFieldStyle.() -> Unit = {}): T {
    this.textFieldStyle.apply(init)
    return this
}

/**
 * Specification for TextField element
 */
open class TextFieldSpec<T : TextField>(
    var textFieldStyle: (TextField.TextFieldStyle.() -> Unit)? = null,
    var text: Any? = null,
    var placeholder: Component? = null,
    var textValidator: Predicate<String>? = null,
    var charValidator: Predicate<Char>? = null,
    var textResponder: Consumer<String>? = null,
    var formatter: Function<String, Component>? = null,
) : ElementSpec<T>() {
    /**
     * Set placeholder text
     */
    fun placeholder(text: String, translate: Boolean = true) = apply {
        this.placeholder = if (translate) Component.translatable(text) else Component.literal(text)
    }

    /**
     * Set regex validation pattern
     */
    fun regex(pattern: String) = apply {
        // Will be applied in applyTextFieldProperties
    }

    /**
     * Convenience alias for textResponder
     */
    fun onTextChanged(handler: Consumer<String>) = apply {
        this.textResponder = handler
    }

    /**
     * Kotlin lambda-friendly text change listener
     */
    fun onTextChanged(handler: (String) -> Unit) = apply {
        this.textResponder = Consumer { handler(it) }
    }
}

/**
 * TextField element builder
 */
open class TextFieldElement<T : TextField>(
    element: T,
    spec: (TextFieldSpec<T>.() -> Unit)? = null,
) : UIContainer<T, TextFieldSpec<T>>(element, spec) {
    override fun makeSpec(): TextFieldSpec<T>? {
        return spec?.let { TextFieldSpec<T>().apply(it) }
    }

    override fun build(spec: TextFieldSpec<T>?): T {
        val e = super.build(spec)
        applyTextFieldProperties(spec, e)
        return e
    }

    protected fun applyTextFieldProperties(spec: TextFieldSpec<T>?, element: TextField) {
        spec?.textFieldStyle?.let(element.textFieldStyle::apply)
        spec?.text?.let { element.setText(it.toString()) }
        spec?.placeholder?.let { element.textFieldStyle.placeholder(it) }
        spec?.textValidator?.let { element.setTextValidator(it) }
        spec?.charValidator?.let { element.setCharValidator(it) }
        spec?.textResponder?.let { element.setTextResponder(it) }
        spec?.formatter?.let { element.setFormatter(it) }
    }
}

/**
 * Top Level - Create a standalone TextField element
 */
fun textField(spec: (TextFieldSpec<TextField>.() -> Unit)? = null,
              init: TextFieldElement<TextField>.() -> Unit = {}): TextField {
    return TextFieldElement(TextField(), spec).apply(init).build()
}

/**
 * Internal Builder - Add TextField as a child to a container
 */
fun UIContainer<*, *>.textField(spec: (TextFieldSpec<TextField>.() -> Unit)? = null,
                                 init: TextFieldElement<TextField>.() -> Unit = {}) =
    add(TextFieldElement(TextField(), spec), init)

/**
 * DSL converter - Convert existing TextField to DSL builder
 */
fun <T : TextField> T.dsl(spec: (TextFieldSpec<T>.() -> Unit)? = null,
                          init: TextFieldElement<T>.() -> Unit = {}): TextFieldElement<T> {
    return TextFieldElement(this, spec).apply(init)
}


/**
 * Extension: Configure as numeric integer field
 */
inline fun <T : TextField, reified N : Number> TextFieldElement<T>.asNumeric(minValue: N, maxValue: N): TextFieldElement<T> = apply {
    val type = N::class.javaPrimitiveType ?: error("Only primitive numeric types are supported")
    when (type) {
        Int::class.javaPrimitiveType -> {
            element.setNumbersOnlyInt(minValue.toInt(), maxValue.toInt())
        }
        Long::class.javaPrimitiveType -> {
            element.setNumbersOnlyLong(minValue.toLong(), maxValue.toLong())
        }
        Float::class.javaPrimitiveType -> {
            element.setNumbersOnlyFloat(minValue.toFloat(), maxValue.toFloat())
        }
        Double::class.javaPrimitiveType -> {
            element.setNumbersOnlyDouble(minValue.toDouble(), maxValue.toDouble())
        }
        Short::class.javaPrimitiveType -> {
            element.setNumbersOnlyShort(minValue.toShort(), maxValue.toShort())
        }
        Byte::class.javaPrimitiveType -> {
            element.setNumbersOnlyByte(minValue.toByte(), maxValue.toByte())
        }
        else -> error("Only numeric types are supported")
    }
}

/**
 * Extension: Configure as ResourceLocation field
 */
fun <T : TextField> TextFieldElement<T>.asResourceLocation(): TextFieldElement<T> = apply {
    element.setResourceLocationOnly()
}

/**
 * Extension: Configure as CompoundTag field
 */
fun <T : TextField> TextFieldElement<T>.asCompoundTag(): TextFieldElement<T> = apply {
    element.setCompoundTagOnly()
}

/**
 * Extension: Configure with regex validation
 */
fun <T : TextField> TextFieldElement<T>.asRegex(pattern: String): TextFieldElement<T> = apply {
    element.setTextRegexValidator(pattern)
}
