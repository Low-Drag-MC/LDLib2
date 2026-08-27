package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.lowdragmc.lowdraglib2.math.interpolate.Eases
import com.lowdragmc.lowdraglib2.math.interpolate.IEase
import com.lowdragmc.lowdraglib2.utils.ColorUtils
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/** How a value gets from where it is to where it is going. */
@Stable
sealed interface AnimationSpec

/**
 * A fixed-duration curve.
 *
 * <p>Easing is LDLib2's own [IEase], not a new curve vocabulary — the same [Eases] constants the
 * stylesheet transitions and the imperative `StyleAnimation` use.
 */
@Stable
data class TweenSpec(
    val durationMillis: Int = 250,
    val delayMillis: Int = 0,
    val easing: IEase = Eases.LINEAR,
) : AnimationSpec {
    init {
        require(durationMillis >= 0) { "durationMillis must not be negative" }
        require(delayMillis >= 0) { "delayMillis must not be negative" }
    }

    /** Progress at [elapsedMillis], eased, clamped to 0..1. */
    internal fun fractionAt(elapsedMillis: Long): Float {
        if (durationMillis == 0) return 1f
        val active = elapsedMillis - delayMillis
        if (active <= 0L) return 0f
        val linear = (active.toFloat() / durationMillis).coerceAtMost(1f)
        return if (linear >= 1f) 1f else easing.interpolate(linear)
    }

    internal fun isFinished(elapsedMillis: Long) = elapsedMillis - delayMillis >= durationMillis
}

/**
 * A damped spring, with no fixed duration — it arrives when the physics say so.
 *
 * <p>The reason to prefer this over a tween for anything the user interrupts: a spring carries its
 * velocity across a change of target, so redirecting mid-flight keeps moving instead of restarting.
 * A tween has to begin a new curve, which reads as a hitch.
 *
 * @param dampingRatio 1 settles without overshooting; below 1 bounces; above 1 is sluggish.
 * @param stiffness how hard it is pulled towards the target — higher is faster.
 * @param threshold how close counts as arrived, in the value's own units. Too small and it creeps
 *   for a long time; too large and it visibly snaps the last stretch.
 */
@Stable
data class SpringSpec(
    val dampingRatio: Float = 1f,
    val stiffness: Float = 400f,
    val threshold: Float = 0.01f,
) : AnimationSpec {
    init {
        require(dampingRatio > 0f) { "dampingRatio must be positive" }
        require(stiffness > 0f) { "stiffness must be positive" }
        require(threshold > 0f) { "threshold must be positive" }
    }
}

/** Result of running an animation to completion: where it ended up and how fast it was going. */
private class Motion(var value: Float, var velocity: Float)

/**
 * Runs an animation off the frame clock, reporting every frame, and returns the exit velocity.
 *
 * <p>Shared by everything below so there is one place where "what does this spec do per frame" is
 * decided.
 */
private suspend inline fun animateScalar(
    spec: AnimationSpec,
    from: Float,
    to: Float,
    startVelocity: Float,
    crossinline onFrame: (value: Float, velocity: Float) -> Unit,
) {
    val motion = Motion(from, startVelocity)
    var startNanos = -1L
    var lastNanos = -1L
    var previousValue = from
    while (true) {
        var nowNanos = 0L
        withFrameNanos { now ->
            if (startNanos < 0L) {
                startNanos = now
                lastNanos = now
            }
            nowNanos = now
        }
        val elapsedMillis = (nowNanos - startNanos) / 1_000_000L
        val deltaMillis = (nowNanos - lastNanos) / 1_000_000L
        lastNanos = nowNanos

        when (spec) {
            is TweenSpec -> {
                if (spec.isFinished(elapsedMillis)) {
                    onFrame(to, 0f)
                    return
                }
                val next = from + (to - from) * spec.fractionAt(elapsedMillis)
                // A tween has no velocity of its own; measuring it from the last frame is enough to
                // hand something sensible over if this gets interrupted by a spring.
                val measured = if (deltaMillis > 0L) (next - previousValue) / (deltaMillis / 1000f) else 0f
                previousValue = next
                onFrame(next, measured)
            }

            is SpringSpec -> {
                if (stepSpring(spec, motion, to, deltaMillis)) {
                    onFrame(to, 0f)
                    return
                }
                onFrame(motion.value, motion.velocity)
            }
        }
    }
}

/**
 * Integrates a damped harmonic oscillator for one frame. Returns true once it has settled.
 *
 * <p>Sub-stepped, and with a ceiling on how much time one frame may contribute. A stiff spring
 * integrated explicitly goes unstable when the step is large, and "large" is exactly what happens
 * after a lag spike or when a window regains focus — without this the value would fly off instead of
 * settling.
 */
private fun stepSpring(spec: SpringSpec, motion: Motion, target: Float, deltaMillis: Long): Boolean {
    val damping = 2f * spec.dampingRatio * sqrt(spec.stiffness)
    var remaining = min(deltaMillis, MAX_FRAME_MILLIS).toFloat() / 1000f
    while (remaining > 0f) {
        val step = min(remaining, MAX_SUB_STEP_SECONDS)
        val displacement = motion.value - target
        val acceleration = -spec.stiffness * displacement - damping * motion.velocity
        motion.velocity += acceleration * step
        motion.value += motion.velocity * step
        remaining -= step
    }
    return abs(motion.value - target) < spec.threshold && abs(motion.velocity) < spec.threshold
}

private const val MAX_FRAME_MILLIS = 64L
private const val MAX_SUB_STEP_SECONDS = 1f / 240f

/** A spring with the usual defaults; the shorthand most call sites want. */
fun spring(dampingRatio: Float = 1f, stiffness: Float = 400f): SpringSpec =
    SpringSpec(dampingRatio, stiffness)

/**
 * A value that can be driven imperatively from a coroutine.
 *
 * <p>The escape hatch for animation that is not a straight "follow this state" — dragging,
 * sequencing one animation after another, or anything that needs to be interrupted on purpose.
 * Reading [value] inside a composable subscribes to it, so the UI follows.
 *
 * ```kotlin
 * val offset = rememberAnimatable(0f)
 * LaunchedEffect(Unit) {
 *     offset.animateTo(100f, spring(dampingRatio = 0.6f))
 *     offset.animateTo(0f, TweenSpec(400))
 * }
 * ```
 */
@Stable
class Animatable(initialValue: Float) {
    var value: Float by mutableStateOf(initialValue)
        private set

    /** Current rate of change, in units per second. Carried across a change of target. */
    var velocity: Float = 0f
        private set

    /** Jumps straight there, cancelling any motion. */
    fun snapTo(targetValue: Float) {
        value = targetValue
        velocity = 0f
    }

    /**
     * Animates to [targetValue], suspending until it arrives.
     *
     * <p>Cancelling the calling coroutine leaves the value wherever it had reached, with its velocity
     * intact — which is what makes a re-issued `animateTo` continue rather than restart.
     */
    suspend fun animateTo(targetValue: Float, animationSpec: AnimationSpec = spring()) {
        if (value == targetValue) {
            velocity = 0f
            return
        }
        // Written every frame rather than once at the end, so that being cancelled part-way leaves
        // both the position and the speed as they actually were — which is what a re-issued
        // animateTo picks up from.
        animateScalar(animationSpec, value, targetValue, velocity) { next, nextVelocity ->
            value = next
            velocity = nextVelocity
        }
    }
}

@Composable
fun rememberAnimatable(initialValue: Float): Animatable = remember { Animatable(initialValue) }

/**
 * Follows [targetValue], but gets there over time instead of jumping.
 *
 * ```kotlin
 * val alpha by animateFloatAsState(if (expanded) 1f else 0f, TweenSpec(200, easing = Eases.QUAD_OUT))
 * Box(UIModifier.opacity(alpha)) { ... }
 * ```
 *
 * <p>Changing the target mid-flight continues from wherever the value currently is rather than from
 * the old start, so an interrupted animation does not jump. With a [SpringSpec] it keeps its
 * velocity too, so a redirection stays smooth rather than merely continuous.
 *
 * <p>This drives *composition* state: each frame writes a new value, which recomposes the readers
 * and re-applies their modifiers. That is a different mechanism from LDLib2's `StyleAnimation`,
 * which mutates the style bag directly and never involves the composition. Both work; this one
 * composes with `if`/`key` and the rest of the declarative layer, at the cost of a recomposition per
 * frame for whatever reads it.
 */
@Composable
fun animateFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec = TweenSpec(),
    finishedListener: ((Float) -> Unit)? = null,
): State<Float> {
    val animatable = remember { Animatable(targetValue) }
    val onFinished by rememberUpdatedState(finishedListener)
    LaunchedEffect(targetValue, animationSpec) {
        animatable.animateTo(targetValue, animationSpec)
        onFinished?.invoke(targetValue)
    }
    return remember(animatable) { derivedStateOf { animatable.value } }
}

/** [animateFloatAsState] for whole numbers; rounds rather than truncating. */
@Composable
fun animateIntAsState(
    targetValue: Int,
    animationSpec: AnimationSpec = TweenSpec(),
    finishedListener: ((Int) -> Unit)? = null,
): State<Int> {
    val animated = animateFloatAsState(
        targetValue.toFloat(),
        animationSpec,
        finishedListener?.let { listener -> { listener(Math.round(it)) } },
    )
    return remember(animated) { derivedStateOf { Math.round(animated.value) } }
}

/**
 * [animateFloatAsState] for packed ARGB colours.
 *
 * <p>Animates a 0..1 blend rather than the packed value: interpolating the ints directly would let
 * one channel's carry bleed into the next. The blend itself is
 * [ColorUtils.blendOklabColor] — the same function `ColorProperty` uses for stylesheet transitions,
 * so a colour animated from here and one animated by a `transition` rule travel the same path.
 */
@Composable
fun animateColorAsState(
    targetValue: Int,
    animationSpec: AnimationSpec = TweenSpec(),
    finishedListener: ((Int) -> Unit)? = null,
): State<Int> {
    val current = remember { mutableStateOf(targetValue) }
    val origin = remember { mutableStateOf(targetValue) }
    val onFinished by rememberUpdatedState(finishedListener)
    val animatable = remember { Animatable(1f) }

    LaunchedEffect(targetValue, animationSpec) {
        if (current.value == targetValue) {
            onFinished?.invoke(targetValue)
            return@LaunchedEffect
        }
        origin.value = current.value
        animatable.snapTo(0f)
        animatable.animateTo(1f, animationSpec)
        current.value = targetValue
        onFinished?.invoke(targetValue)
    }

    return remember(animatable) {
        derivedStateOf {
            if (animatable.value >= 1f) current.value else lerpArgb(origin.value, targetValue, animatable.value)
        }
    }
}

internal fun lerpArgb(from: Int, to: Int, fraction: Float): Int =
    ColorUtils.blendOklabColor(from, to, fraction.coerceIn(0f, 1f))

/**
 * Shows [content] while [visible], fading it in and out.
 *
 * <p>The reason this exists rather than a bare `if (visible)` is the *exit*: removing the content
 * from the composition immediately means there is nothing left to animate out. So it stays composed
 * until the fade finishes, and only then leaves — which is also when its `DisposableEffect`s run.
 */
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    modifier: UIModifier = UIModifier,
    animationSpec: AnimationSpec = TweenSpec(),
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, animationSpec)
    if (visible || alpha > 0f) {
        Box(modifier.opacity(alpha.coerceIn(0f, 1f)), content)
    }
}
