package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.lowdragmc.lowdraglib2.math.interpolate.Eases
import com.lowdragmc.lowdraglib2.math.interpolate.IEase
import com.lowdragmc.lowdraglib2.utils.ColorUtils
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext
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

    /** Progress at [elapsedMillis], eased, clamped to 0..1. Nothing moves until the delay is over. */
    internal fun fractionAt(elapsedMillis: Long): Float {
        val active = elapsedMillis - delayMillis
        if (active < 0L) return 0f
        if (durationMillis == 0) return 1f
        val linear = (active.toFloat() / durationMillis).coerceAtMost(1f)
        return if (linear >= 1f) 1f else easing.interpolate(linear)
    }

    internal fun isFinished(elapsedMillis: Long) = elapsedMillis - delayMillis >= durationMillis
}

/** A tween; the shorthand next to [spring]. */
fun tween(durationMillis: Int = 250, delayMillis: Int = 0, easing: IEase = Eases.LINEAR): TweenSpec =
    TweenSpec(durationMillis, delayMillis, easing)

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

/** A spring with the usual defaults; the shorthand most call sites want. */
fun spring(dampingRatio: Float = 1f, stiffness: Float = 400f, threshold: Float = 0.01f): SpringSpec =
    SpringSpec(dampingRatio, stiffness, threshold)

/** The integrator's live state for one run of [animateScalar]: where the value is and how fast. */
private class Motion(var value: Float, var velocity: Float)

/**
 * Runs an animation off the frame clock, reporting value and velocity every frame, until it arrives.
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
 * <p>Semi-implicit Euler, sub-stepped finely enough for the spring at hand. Explicit integration of a
 * stiff or heavily damped spring goes unstable once the step outgrows the spring's own time scale, so
 * the step shrinks with both the natural frequency and the damping. A frame may also only contribute
 * so much time: a lag spike, or a window regaining focus, would otherwise integrate a second in one
 * go. And should the value still go non-finite, it settles on the target rather than drawing NaN.
 */
private fun stepSpring(spec: SpringSpec, motion: Motion, target: Float, deltaMillis: Long): Boolean {
    val omega = sqrt(spec.stiffness)
    val damping = 2f * spec.dampingRatio * omega
    val maxStep = min(MAX_SUB_STEP_SECONDS, 0.5f / (spec.dampingRatio * omega + omega))
    var remaining = min(deltaMillis, MAX_FRAME_MILLIS).toFloat() / 1000f
    while (remaining > 0f) {
        val step = min(remaining, maxStep)
        val displacement = motion.value - target
        val acceleration = -spec.stiffness * displacement - damping * motion.velocity
        motion.velocity += acceleration * step
        motion.value += motion.velocity * step
        remaining -= step
    }
    if (!motion.value.isFinite() || !motion.velocity.isFinite()) {
        motion.value = target
        motion.velocity = 0f
        return true
    }
    return abs(motion.value - target) < spec.threshold && abs(motion.velocity) < spec.threshold
}

private const val MAX_FRAME_MILLIS = 64L
private const val MAX_SUB_STEP_SECONDS = 1f / 240f

/**
 * A value that can be driven imperatively from a coroutine.
 *
 * <p>The escape hatch for animation that is not a straight "follow this state" — dragging,
 * sequencing one animation after another, or anything that needs to be interrupted on purpose.
 * Reading [value] inside a composable subscribes to it, so the UI follows.
 *
 * <p>One animation at a time: starting another, or calling [snapTo], cancels the one running — its
 * coroutine ends with a `CancellationException` — and the new one carries on from where it left the
 * value.
 *
 * ```kotlin
 * val offset = rememberAnimatable(0f)
 * LaunchedEffect(Unit) {
 *     offset.animateTo(100f, spring(dampingRatio = 0.6f))
 *     offset.animateTo(0f, tween(400))
 * }
 * ```
 */
@Stable
class Animatable(initialValue: Float) {
    var value: Float by mutableFloatStateOf(initialValue)
        private set

    /** Current rate of change, in units per second. Carried across a change of target. */
    var velocity: Float = 0f
        private set

    private var running: Job? = null

    /** Jumps straight there, cancelling any animation in progress. */
    fun snapTo(targetValue: Float) {
        running?.cancel()
        running = null
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
        val job = coroutineContext[Job]
        running?.takeIf { it !== job }?.cancel()
        running = job
        try {
            if (value == targetValue) {
                velocity = 0f
                return
            }
            // Written every frame rather than once at the end, so that being cancelled part-way
            // leaves both the position and the speed as they actually were — which is what a
            // re-issued animateTo picks up from.
            animateScalar(animationSpec, value, targetValue, velocity) { next, nextVelocity ->
                value = next
                velocity = nextVelocity
            }
        } finally {
            if (running === job) running = null
        }
    }
}

@Composable
fun rememberAnimatable(initialValue: Float): Animatable = remember { Animatable(initialValue) }

/**
 * Follows [targetValue], but gets there over time instead of jumping.
 *
 * ```kotlin
 * val alpha by animateFloatAsState(if (expanded) 1f else 0f, tween(200, easing = Eases.QUAD_OUT))
 * Box(style = { opacity(alpha) }) { ... }
 * ```
 *
 * <p>Changing the target mid-flight continues from wherever the value currently is rather than from
 * the old start, so an interrupted animation does not jump. With a [SpringSpec] it keeps its
 * velocity too, so a redirection stays smooth rather than merely continuous.
 *
 * <p>Only a new target restarts the animation. A spec that changes along the way — or is simply a new
 * instance each time, as a capturing easing lambda makes it — applies from the next target on.
 *
 * <p>Read inside a `style` / `layout` block, as above, the value updates that one block every frame
 * without recomposing anything; read in a composable body, that body recomposes every frame.
 *
 * @param finishedListener called with the target each time an animation to it completes. Not called
 *   for the initial value, nor for an animation interrupted by a new target.
 */
@Composable
fun animateFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec = TweenSpec(),
    finishedListener: ((Float) -> Unit)? = null,
): State<Float> {
    val animatable = remember { Animatable(targetValue) }
    val spec by rememberUpdatedState(animationSpec)
    val onFinished by rememberUpdatedState(finishedListener)
    LaunchedEffect(targetValue) {
        if (animatable.value == targetValue) return@LaunchedEffect
        animatable.animateTo(targetValue, spec)
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
    val onFinished by rememberUpdatedState(finishedListener)
    val animated = animateFloatAsState(
        targetValue.toFloat(),
        animationSpec,
        // A stable lambda reading the latest listener, so passing one does not hand
        // animateFloatAsState a new instance on every recomposition.
        remember { { value: Float -> onFinished?.invoke(Math.round(value)) } },
    )
    return remember(animated) { derivedStateOf { Math.round(animated.value) } }
}

/**
 * [animateFloatAsState] for packed ARGB colours.
 *
 * <p>Animates a 0..1 blend rather than the packed value: interpolating the ints directly would let
 * one channel's carry bleed into the next. The blend itself is [ColorUtils.blendOklabColor] — the same
 * function `ColorProperty` uses for stylesheet transitions, so a colour animated from here and one
 * animated by a `transition` rule travel the same path.
 *
 * <p>A new target blends from the colour on screen at that moment, so changing it mid-flight — or
 * going back to where it started — continues smoothly instead of jumping.
 */
@Composable
fun animateColorAsState(
    targetValue: Int,
    animationSpec: AnimationSpec = TweenSpec(),
    finishedListener: ((Int) -> Unit)? = null,
): State<Int> {
    val blend = remember { ColorBlend(targetValue) }
    val spec by rememberUpdatedState(animationSpec)
    val onFinished by rememberUpdatedState(finishedListener)
    LaunchedEffect(targetValue) {
        if (blend.to == targetValue && blend.progress.value >= 1f) return@LaunchedEffect
        blend.retarget(targetValue)
        blend.progress.animateTo(1f, spec)
        onFinished?.invoke(targetValue)
    }
    return remember(blend) { derivedStateOf { blend.current() } }
}

/** A colour on its way from [from] to [to], [progress] of the way there. */
@Stable
private class ColorBlend(initial: Int) {
    var from by mutableIntStateOf(initial)
        private set
    var to by mutableIntStateOf(initial)
        private set
    val progress = Animatable(1f)

    fun current(): Int = if (progress.value >= 1f) to else lerpArgb(from, to, progress.value)

    /** Starts again from what is on screen now, towards [target]. */
    fun retarget(target: Int) {
        from = current()
        to = target
        progress.snapTo(0f)
    }
}

internal fun lerpArgb(from: Int, to: Int, fraction: Float): Int =
    ColorUtils.blendOklabColor(from, to, fraction.coerceIn(0f, 1f))
