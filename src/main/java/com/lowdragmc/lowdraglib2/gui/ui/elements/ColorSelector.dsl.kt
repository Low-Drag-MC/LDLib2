package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import java.util.function.IntConsumer

/**
 * Specification for ColorSelector element
 */
open class ColorSelectorSpec<T : ColorSelector>(
    var initialColor: Int? = null,
    var onColorChanged: IntConsumer? = null,
) : ElementSpec<T>() {
    /**
     * Set initial color (ARGB)
     */
    fun color(argb: Int) = apply {
        this.initialColor = argb
    }

    /**
     * Set initial color (RGB with full alpha)
     */
    fun colorRgb(rgb: Int) = apply {
        this.initialColor = 0xFF000000.toInt() or rgb
    }

    /**
     * Set color change listener
     */
    fun onChange(handler: IntConsumer) = apply {
        this.onColorChanged = handler
    }

    /**
     * Set color change listener (Kotlin lambda)
     */
    fun onChange(handler: (Int) -> Unit) = apply {
        this.onColorChanged = IntConsumer { handler(it) }
    }
}

/**
 * ColorSelector element builder
 */
open class ColorSelectorElement<T : ColorSelector>(
    element: T,
    spec: (ColorSelectorSpec<T>.() -> Unit)? = null,
) : UIContainer<T, ColorSelectorSpec<T>>(element, spec) {
    override fun makeSpec(): ColorSelectorSpec<T>? {
        return spec?.let { ColorSelectorSpec<T>().apply(it) }
    }

    override fun build(spec: ColorSelectorSpec<T>?): T {
        val e = super.build(spec)
        applyColorSelectorProperties(spec, e)
        return e
    }

    protected fun applyColorSelectorProperties(spec: ColorSelectorSpec<T>?, element: ColorSelector) {
        spec?.initialColor?.let { element.setColor(it, false) }
        spec?.onColorChanged?.let { element.setOnColorChangeListener(it) }
    }
}

/**
 * Top Level - Create a standalone ColorSelector element
 */
fun colorSelector(spec: (ColorSelectorSpec<ColorSelector>.() -> Unit)? = null,
                  init: ColorSelectorElement<ColorSelector>.() -> Unit = {}): ColorSelector {
    return ColorSelectorElement(ColorSelector(), spec).apply(init).build()
}

/**
 * Internal Builder - Add ColorSelector as a child to a container
 */
fun UIContainer<*, *>.colorSelector(spec: (ColorSelectorSpec<ColorSelector>.() -> Unit)? = null,
                                     init: ColorSelectorElement<ColorSelector>.() -> Unit = {}) =
    add(ColorSelectorElement(ColorSelector(), spec), init)

/**
 * DSL converter - Convert existing ColorSelector to DSL builder
 */
fun <T : ColorSelector> T.dsl(spec: (ColorSelectorSpec<T>.() -> Unit)? = null,
                              init: ColorSelectorElement<T>.() -> Unit = {}): ColorSelectorElement<T> {
    return ColorSelectorElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set color (ARGB)
 */
fun <T : ColorSelector> ColorSelectorElement<T>.withColor(argb: Int): ColorSelectorElement<T> = apply {
    element.setColor(argb, false)
}

/**
 * Extension: Set color (RGB with full alpha)
 */
fun <T : ColorSelector> ColorSelectorElement<T>.withColorRgb(rgb: Int): ColorSelectorElement<T> = apply {
    element.setColor(0xFF000000.toInt() or rgb, false)
}

/**
 * Extension: Get current color
 */
fun <T : ColorSelector> ColorSelectorElement<T>.getColor(): Int {
    return element.getColor()
}

/**
 * Extension: Set color change listener
 */
fun <T : ColorSelector> ColorSelectorElement<T>.onColorChange(handler: (Int) -> Unit): ColorSelectorElement<T> = apply {
    element.setOnColorChangeListener(IntConsumer { handler(it) })
}

/**
 * Extension: Configure picker container
 */
fun <T : ColorSelector> ColorSelectorElement<T>.withPickerContainer(config: UIElement.() -> Unit): ColorSelectorElement<T> = apply {
    element.pickerContainer.apply(config)
}

/**
 * Extension: Configure color preview
 */
fun <T : ColorSelector> ColorSelectorElement<T>.withColorPreview(config: UIElement.() -> Unit): ColorSelectorElement<T> = apply {
    element.colorPreview.apply(config)
}

/**
 * Extension: Configure HSB button
 */
fun <T : ColorSelector> ColorSelectorElement<T>.withHsbButton(config: Button.() -> Unit): ColorSelectorElement<T> = apply {
    element.hsbButton.apply(config)
}
