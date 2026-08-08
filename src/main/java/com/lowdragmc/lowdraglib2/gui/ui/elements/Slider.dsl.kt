package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import java.util.function.Function

/**
 * Extension function for Slider.SliderStyle DSL
 */
fun <T : Slider> T.sliderStyleDsl(init: Slider.SliderStyle.() -> Unit = {}): T {
    this.sliderStyle.apply(init)
    return this
}

/**
 * Base specification for Slider elements
 */
open class SliderSpec<T : Slider>(
    var sliderStyle: (Slider.SliderStyle.() -> Unit)? = null,
    var minValue: Float? = null,
    var maxValue: Float? = null,
    var value: Float? = null,
    var normalizedValue: Float? = null,
    var sliderStep: Float? = null,
    var trackSize: Float? = null,
    var handleSize: Float? = null,
    var onValueChanged: ((Float) -> Unit)? = null,
    var clampNormalizedValue: Function<Float, Float>? = null,
) : ElementSpec<T>() {
    /**
     * Set the range of the slider
     */
    fun range(min: Float, max: Float) = apply {
        this.minValue = min
        this.maxValue = max
    }

    /**
     * Set how far one wheel notch moves the value, as a fraction of the range
     */
    fun step(step: Float) = apply {
        this.sliderStep = step
    }

    /**
     * Set the thickness of the line, in pixels
     */
    fun track(size: Float) = apply {
        this.trackSize = size
    }

    /**
     * Set the length of the handle along the track, in pixels
     */
    fun handle(size: Float) = apply {
        this.handleSize = size
    }

    /**
     * Set value change listener
     */
    fun onChange(handler: (Float) -> Unit) = apply {
        this.onValueChanged = handler
    }
}

/**
 * Base Slider element builder
 */
open class SliderElement<T : Slider>(
    element: T,
    spec: (SliderSpec<T>.() -> Unit)? = null,
) : UIContainer<T, SliderSpec<T>>(element, spec) {
    override fun makeSpec(): SliderSpec<T>? {
        return spec?.let { SliderSpec<T>().apply(it) }
    }

    override fun build(spec: SliderSpec<T>?): T {
        val e = super.build(spec)
        applySliderProperties(spec, e)
        return e
    }

    protected fun applySliderProperties(spec: SliderSpec<T>?, element: Slider) {
        spec?.sliderStyle?.let(element.sliderStyle::apply)

        // Apply range first if both min and max are specified
        if (spec?.minValue != null || spec?.maxValue != null) {
            val min = spec.minValue ?: element.minValue
            val max = spec.maxValue ?: element.maxValue
            element.setRange(min, max)
        }

        // Apply value or normalizedValue
        if (spec?.normalizedValue != null) {
            spec.normalizedValue?.let { element.setNormalizedValue(it) }
        } else {
            spec?.value?.let { element.setValue(it) }
        }

        spec?.sliderStep?.let { element.sliderStyle.sliderStep(it) }
        spec?.trackSize?.let { element.sliderStyle.trackSize(it) }
        spec?.handleSize?.let { element.sliderStyle.handleSize(it) }
        spec?.onValueChanged?.let { handler ->
            element.setOnValueChanged { handler(it) }
        }
        spec?.clampNormalizedValue?.let { element.setClampNormalizedValue(it) }
    }
}

// ============================================
// Horizontal Slider (slider-horizontal)
// ============================================

/**
 * Specification for Horizontal Slider
 */
open class SliderHorizontalSpec : SliderSpec<Slider.Horizontal>()

/**
 * Horizontal Slider element builder
 */
open class SliderHorizontalElement(
    element: Slider.Horizontal,
    spec: (SliderSpec<Slider.Horizontal>.() -> Unit)? = null,
) : SliderElement<Slider.Horizontal>(element, spec) {
    override fun makeSpec(): SliderHorizontalSpec? {
        return spec?.let { SliderHorizontalSpec().apply(it) }
    }
}

/**
 * Top Level - Create a standalone Horizontal Slider
 */
fun sliderHorizontal(spec: (SliderSpec<Slider.Horizontal>.() -> Unit)? = null,
                     init: SliderHorizontalElement.() -> Unit = {}): Slider.Horizontal {
    return SliderHorizontalElement(Slider.Horizontal(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Horizontal Slider as a child to a container
 */
fun UIContainer<*, *>.sliderHorizontal(spec: (SliderSpec<Slider.Horizontal>.() -> Unit)? = null,
                                       init: SliderHorizontalElement.() -> Unit = {}) =
    add(SliderHorizontalElement(Slider.Horizontal(), spec), init)

/**
 * DSL converter - Convert existing Horizontal Slider to DSL builder
 */
fun Slider.Horizontal.dsl(spec: (SliderSpec<Slider.Horizontal>.() -> Unit)? = null,
                          init: SliderHorizontalElement.() -> Unit = {}): SliderHorizontalElement {
    return SliderHorizontalElement(this, spec).apply(init)
}

// ============================================
// Vertical Slider (slider-vertical)
// ============================================

/**
 * Specification for Vertical Slider
 */
open class SliderVerticalSpec : SliderSpec<Slider.Vertical>()

/**
 * Vertical Slider element builder
 */
open class SliderVerticalElement(
    element: Slider.Vertical,
    spec: (SliderSpec<Slider.Vertical>.() -> Unit)? = null,
) : SliderElement<Slider.Vertical>(element, spec) {
    override fun makeSpec(): SliderVerticalSpec? {
        return spec?.let { SliderVerticalSpec().apply(it) }
    }
}

/**
 * Top Level - Create a standalone Vertical Slider
 */
fun sliderVertical(spec: (SliderSpec<Slider.Vertical>.() -> Unit)? = null,
                   init: SliderVerticalElement.() -> Unit = {}): Slider.Vertical {
    return SliderVerticalElement(Slider.Vertical(), spec).apply(init).build()
}

/**
 * Internal Builder - Add Vertical Slider as a child to a container
 */
fun UIContainer<*, *>.sliderVertical(spec: (SliderSpec<Slider.Vertical>.() -> Unit)? = null,
                                     init: SliderVerticalElement.() -> Unit = {}) =
    add(SliderVerticalElement(Slider.Vertical(), spec), init)

/**
 * DSL converter - Convert existing Vertical Slider to DSL builder
 */
fun Slider.Vertical.dsl(spec: (SliderSpec<Slider.Vertical>.() -> Unit)? = null,
                        init: SliderVerticalElement.() -> Unit = {}): SliderVerticalElement {
    return SliderVerticalElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set slider range
 */
fun <T : Slider> SliderElement<T>.withRange(min: Float, max: Float): SliderElement<T> = apply {
    element.setRange(min, max)
}

/**
 * Extension: Set slider value
 */
fun <T : Slider> SliderElement<T>.withValue(value: Float): SliderElement<T> = apply {
    element.setValue(value)
}

/**
 * Extension: Set normalized value (0-1)
 */
fun <T : Slider> SliderElement<T>.withNormalizedValue(value: Float): SliderElement<T> = apply {
    element.setNormalizedValue(value)
}

/**
 * Extension: Set the step of one wheel notch
 */
fun <T : Slider> SliderElement<T>.withStep(step: Float): SliderElement<T> = apply {
    element.sliderStyle.sliderStep(step)
}

/**
 * Extension: Set the thickness of the line
 */
fun <T : Slider> SliderElement<T>.withTrackSize(size: Float): SliderElement<T> = apply {
    element.sliderStyle.trackSize(size)
}

/**
 * Extension: Set the length of the handle
 */
fun <T : Slider> SliderElement<T>.withHandleSize(size: Float): SliderElement<T> = apply {
    element.sliderStyle.handleSize(size)
}

/**
 * Extension: Set value change callback
 */
fun <T : Slider> SliderElement<T>.onValueChange(handler: (Float) -> Unit): SliderElement<T> = apply {
    element.setOnValueChanged { handler(it) }
}

/**
 * Extension: Set clamp function for normalized value
 */
fun <T : Slider> SliderElement<T>.withClamp(clamp: Function<Float, Float>): SliderElement<T> = apply {
    element.setClampNormalizedValue(clamp)
}
