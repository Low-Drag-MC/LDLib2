package com.lowdragmc.lowdraglib2.gui.ui.style.animation

import com.lowdragmc.lowdraglib2.gui.ui.UIDslMarker
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.style.Property
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin
import com.lowdragmc.lowdraglib2.math.interpolate.IEase
import com.lowdragmc.lowdraglib2.syncdata.ISubscription
import com.lowdragmc.lowdraglib2.utils.animation.AnimationRuntime
import it.unimi.dsi.fastutil.floats.FloatObjectPair
import java.util.function.BiConsumer
import java.util.function.Consumer

data class StyleKeyFrame<T>(val progress: Float, val value: T)

@UIDslMarker
class StyleAnimationDsl internal constructor(
    private val animation: StyleAnimation,
) {
    fun duration(value: Number) = apply { animation.duration(value.toFloat()) }

    fun delay(value: Number) = apply { animation.delay(value.toFloat()) }

    fun ease(value: IEase) = apply { animation.ease(value) }

    fun origin(value: StyleOrigin) = apply { animation.origin(value) }

    fun animationOrigin(value: StyleOrigin) = apply { animation.animationOrigin(value) }

    fun specificity(value: Int) = apply { animation.specificity(value) }

    fun sourceOrder(value: Int) = apply { animation.sourceOrder(value) }

    fun select(element: UIElement) = apply { animation.select(element) }

    fun select(selector: String) = apply { animation.select(selector) }

    fun <T> style(property: Property<T>, value: T) = apply { animation.style(property, value) }

    fun <T> style(property: Property<T>, vararg frames: StyleKeyFrame<T>) = apply {
        val values = frames.map { FloatObjectPair.of(it.progress, it.value) }.toTypedArray()
        animation.style(property, *values)
    }

    fun <T> to(property: Property<T>, value: T) = style(property, value)

    fun <T> fromTo(property: Property<T>, from: T, to: T) = style(property, at(0f, from), at(1f, to))

    fun lss(property: String, value: Any) = apply { animation.lss(property, value) }

    fun onInterpolate(handler: (AnimationRuntime, UIElement) -> Unit) = apply {
        animation.onInterpolate(BiConsumer { runtime, element -> handler(runtime, element) })
    }

    fun onFinished(handler: (UIElement) -> Unit) = apply {
        animation.onFinished(Consumer { element -> handler(element) })
    }

    fun start(): ISubscription = animation.start()

    fun <T> at(progress: Number, value: T): StyleKeyFrame<T> = StyleKeyFrame(progress.toFloat(), value)
}

fun <T> at(progress: Number, value: T): StyleKeyFrame<T> = StyleKeyFrame(progress.toFloat(), value)
