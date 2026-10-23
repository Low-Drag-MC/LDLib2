package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.auto
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import dev.vfyjxf.taffy.style.FlexDirection
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

internal class Fade(val alpha: Float, val spec: AnimationSpec)

/** Offsets are fractions of the element's own size, resolved when it is drawn. */
internal class Slide(val offsetX: Float, val offsetY: Float, val spec: AnimationSpec)

internal class Scale(val scale: Float, val pivotX: Float, val pivotY: Float, val spec: AnimationSpec)

internal class ChangeSize(val vertical: Boolean, val spec: AnimationSpec)

/** One side of a transition: which properties it animates, from or to what, and how. */
@Immutable
internal class TransitionData(
    val fade: Fade? = null,
    val slide: Slide? = null,
    val scale: Scale? = null,
    val size: ChangeSize? = null,
) {
    /** Left wins where both sides animate the same property. */
    operator fun plus(other: TransitionData) = TransitionData(
        fade ?: other.fade,
        slide ?: other.slide,
        scale ?: other.scale,
        size ?: other.size,
    )
}

/**
 * How [AnimatedVisibility] brings content in. Built from [fadeIn], [slideIn], [scaleIn] and
 * [expandVertically] and their relatives, and combined with `+`:
 *
 * ```kotlin
 * enter = fadeIn() + slideInVertically() + expandVertically()
 * ```
 *
 * <p>Everything combined runs at once, each part on its own spec. Where two parts animate the same
 * property — two fades — the left one wins.
 */
@Immutable
class EnterTransition internal constructor(internal val data: TransitionData) {
    operator fun plus(enter: EnterTransition): EnterTransition = EnterTransition(data + enter.data)

    companion object {
        /** Appears at once. */
        val None = EnterTransition(TransitionData())
    }
}

/** How [AnimatedVisibility] takes content away. The mirror of [EnterTransition]. */
@Immutable
class ExitTransition internal constructor(internal val data: TransitionData) {
    operator fun plus(exit: ExitTransition): ExitTransition = ExitTransition(data + exit.data)

    companion object {
        /** Disappears at once. */
        val None = ExitTransition(TransitionData())
    }
}

/** Fades in from [initialAlpha]. */
fun fadeIn(animationSpec: AnimationSpec = TweenSpec(), initialAlpha: Float = 0f): EnterTransition =
    EnterTransition(TransitionData(fade = Fade(initialAlpha, animationSpec)))

/** Fades out to [targetAlpha]. */
fun fadeOut(animationSpec: AnimationSpec = TweenSpec(), targetAlpha: Float = 0f): ExitTransition =
    ExitTransition(TransitionData(fade = Fade(targetAlpha, animationSpec)))

/**
 * Slides in from an offset, given as a fraction of the content's own size: `-1f` on x starts one full
 * width to the left. A fraction rather than pixels because the size is not known until the content
 * has been laid out, and it is resolved when the content is drawn, so the first frame is right.
 *
 * <p>Drawing only. The content keeps its place in the layout throughout, so neighbours do not move;
 * pair it with [expandVertically] for content that should also push them aside.
 */
fun slideIn(
    animationSpec: AnimationSpec = TweenSpec(),
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
): EnterTransition = EnterTransition(TransitionData(slide = Slide(initialOffsetX, initialOffsetY, animationSpec)))

/** Slides out to an offset, as a fraction of the content's own size. See [slideIn]. */
fun slideOut(
    animationSpec: AnimationSpec = TweenSpec(),
    targetOffsetX: Float = 0f,
    targetOffsetY: Float = 0f,
): ExitTransition = ExitTransition(TransitionData(slide = Slide(targetOffsetX, targetOffsetY, animationSpec)))

/** [slideIn] along x. The default starts half a width to the left. */
fun slideInHorizontally(animationSpec: AnimationSpec = TweenSpec(), initialOffsetX: Float = -0.5f): EnterTransition =
    slideIn(animationSpec, initialOffsetX = initialOffsetX)

/** [slideIn] along y. The default starts half a height above. */
fun slideInVertically(animationSpec: AnimationSpec = TweenSpec(), initialOffsetY: Float = -0.5f): EnterTransition =
    slideIn(animationSpec, initialOffsetY = initialOffsetY)

/** [slideOut] along x. The default leaves half a width to the left. */
fun slideOutHorizontally(animationSpec: AnimationSpec = TweenSpec(), targetOffsetX: Float = -0.5f): ExitTransition =
    slideOut(animationSpec, targetOffsetX = targetOffsetX)

/** [slideOut] along y. The default leaves half a height upwards. */
fun slideOutVertically(animationSpec: AnimationSpec = TweenSpec(), targetOffsetY: Float = -0.5f): ExitTransition =
    slideOut(animationSpec, targetOffsetY = targetOffsetY)

/**
 * Grows from [initialScale] about a pivot given as fractions of the content's size — the centre by
 * default. Drawing only, like [slideIn].
 */
fun scaleIn(
    animationSpec: AnimationSpec = TweenSpec(),
    initialScale: Float = 0f,
    pivotX: Float = 0.5f,
    pivotY: Float = 0.5f,
): EnterTransition = EnterTransition(TransitionData(scale = Scale(initialScale, pivotX, pivotY, animationSpec)))

/** Shrinks to [targetScale]. See [scaleIn]. */
fun scaleOut(
    animationSpec: AnimationSpec = TweenSpec(),
    targetScale: Float = 0f,
    pivotX: Float = 0.5f,
    pivotY: Float = 0.5f,
): ExitTransition = ExitTransition(TransitionData(scale = Scale(targetScale, pivotX, pivotY, animationSpec)))

/**
 * Grows the space the content takes from nothing to its full height, revealing it from the top, so
 * whatever sits below is pushed down smoothly rather than jumping.
 *
 * <p>Unlike [slideIn] and [scaleIn] this changes layout, so it costs a little more: the content is
 * wrapped once more so it can be measured, and it is clipped to the animated height while it runs.
 */
fun expandVertically(animationSpec: AnimationSpec = TweenSpec()): EnterTransition =
    EnterTransition(TransitionData(size = ChangeSize(vertical = true, animationSpec)))

/** Gives the content's height back to the layout. See [expandVertically]. */
fun shrinkVertically(animationSpec: AnimationSpec = TweenSpec()): ExitTransition =
    ExitTransition(TransitionData(size = ChangeSize(vertical = true, animationSpec)))

/** [expandVertically] across: grows from nothing to the content's own width. */
fun expandHorizontally(animationSpec: AnimationSpec = TweenSpec()): EnterTransition =
    EnterTransition(TransitionData(size = ChangeSize(vertical = false, animationSpec)))

/** Gives the content's width back to the layout. See [expandHorizontally]. */
fun shrinkHorizontally(animationSpec: AnimationSpec = TweenSpec()): ExitTransition =
    ExitTransition(TransitionData(size = ChangeSize(vertical = false, animationSpec)))

/**
 * Where an [AnimatedVisibility] is: whether its content belongs in the tree, and the current value of
 * everything it animates.
 *
 * <p>Separate from the composable so the timing can be tested without an element tree.
 *
 * <p>Each property is its own [Animatable] holding the real value, not a 0..1 progress mapped through
 * the transition. That is what keeps an interruption smooth: reversing mid-flight continues from
 * where the value is, even when the entrance and the exit go in different directions.
 *
 * <p>While hidden, every property rests where the *next entrance* begins. Setting that up when an
 * exit finishes, rather than when the next entrance starts, is deliberate: a value written from an
 * effect only reaches the element a frame later, so correcting it on entry would draw one frame at
 * the old exit position first.
 */
@Stable
internal class VisibilityTransition(visible: Boolean, enter: TransitionData) {
    val alpha = Animatable(1f)
    val offsetX = Animatable(0f)
    val offsetY = Animatable(0f)
    val scale = Animatable(1f)

    /** How much of the content's full extent is showing, 0..1, along the [ChangeSize] axis. */
    val size = Animatable(1f)

    var pivotX by mutableStateOf(0.5f)
        private set
    var pivotY by mutableStateOf(0.5f)
        private set

    /** The content's own extent along the [ChangeSize] axis, as last measured. */
    var fullSize by mutableStateOf(0f)
        private set

    /** True while hidden and every exit has run to the end — the one state with nothing to draw. */
    var exited by mutableStateOf(!visible)
        private set

    init {
        if (!visible) restAtStartOf(enter)
    }

    fun isMounted(visible: Boolean) = visible || !exited

    fun measure(extent: Float) {
        // Same half-pixel tolerance as the lazy lists, for the same reason: a value wobbling in the
        // layout noise would otherwise re-apply the blocks every frame.
        if (extent > 0f && abs(extent - fullSize) >= 0.5f) fullSize = extent
    }

    suspend fun enter(enter: TransitionData, exit: TransitionData) {
        if (exited) {
            // Normally a no-op, the exit having left everything here already. Not when `enter`
            // changed while the content was hidden.
            restAtStartOf(enter)
            exited = false
        }
        (enter.scale ?: exit.scale)?.let { usePivot(it) }
        coroutineScope {
            launch { alpha.settleTo(1f, enter.fade?.spec ?: exit.fade?.spec) }
            launch { offsetX.settleTo(0f, enter.slide?.spec ?: exit.slide?.spec) }
            launch { offsetY.settleTo(0f, enter.slide?.spec ?: exit.slide?.spec) }
            launch { scale.settleTo(1f, enter.scale?.spec ?: exit.scale?.spec) }
            launch { size.settleTo(1f, enter.size?.spec ?: exit.size?.spec) }
        }
    }

    suspend fun exit(enter: TransitionData, exit: TransitionData) {
        if (exited) return
        (exit.scale ?: enter.scale)?.let { usePivot(it) }
        // A property the exit does not mention goes back to its visible value, on whichever spec the
        // entrance used for it — it only differs from that value if an entrance was interrupted.
        coroutineScope {
            launch { alpha.settleTo(exit.fade?.alpha ?: 1f, exit.fade?.spec ?: enter.fade?.spec) }
            launch { offsetX.settleTo(exit.slide?.offsetX ?: 0f, exit.slide?.spec ?: enter.slide?.spec) }
            launch { offsetY.settleTo(exit.slide?.offsetY ?: 0f, exit.slide?.spec ?: enter.slide?.spec) }
            launch { scale.settleTo(exit.scale?.scale ?: 1f, exit.scale?.spec ?: enter.scale?.spec) }
            launch { size.settleTo(if (exit.size != null) 0f else 1f, exit.size?.spec ?: enter.size?.spec) }
        }
        restAtStartOf(enter)
        exited = true
    }

    private fun restAtStartOf(enter: TransitionData) {
        alpha.snapTo(enter.fade?.alpha ?: 1f)
        offsetX.snapTo(enter.slide?.offsetX ?: 0f)
        offsetY.snapTo(enter.slide?.offsetY ?: 0f)
        scale.snapTo(enter.scale?.scale ?: 1f)
        size.snapTo(if (enter.size != null) 0f else 1f)
        enter.scale?.let { usePivot(it) }
    }

    private fun usePivot(scale: Scale) {
        pivotX = scale.pivotX
        pivotY = scale.pivotY
    }

    private suspend fun Animatable.settleTo(target: Float, spec: AnimationSpec?) {
        if (spec == null) snapTo(target) else animateTo(target, spec)
    }
}

@Composable
internal fun rememberVisibilityTransition(
    visible: Boolean,
    enter: EnterTransition,
    exit: ExitTransition,
): VisibilityTransition {
    val transition = remember { VisibilityTransition(visible, enter.data) }
    val currentEnter by rememberUpdatedState(enter.data)
    val currentExit by rememberUpdatedState(exit.data)
    LaunchedEffect(visible) {
        if (visible) {
            transition.enter(currentEnter, currentExit)
        } else {
            transition.exit(currentEnter, currentExit)
        }
    }
    return transition
}

/**
 * Which axis the content's size animates along: true for vertical, false for horizontal, null when
 * neither side changes size.
 */
internal fun sizeAxis(enter: TransitionData, exit: TransitionData): Boolean? {
    val enterAxis = enter.size?.vertical
    val exitAxis = exit.size?.vertical
    require(enterAxis == null || exitAxis == null || enterAxis == exitAxis) {
        "AnimatedVisibility can expand and shrink along one axis, not one on the way in and another on the way out"
    }
    return enterAxis ?: exitAxis
}

/**
 * Shows [content] while [visible], animating it in and out.
 *
 * ```kotlin
 * AnimatedVisibility(
 *     visible = expanded,
 *     enter = fadeIn() + expandVertically(),
 *     exit = fadeOut() + shrinkVertically(),
 * ) { Details() }
 * ```
 *
 * <p>The reason this exists rather than a bare `if (visible)` is the *exit*: removing the content
 * from the composition immediately means there is nothing left to animate out. So it stays composed
 * until every part of [exit] has finished, and only then leaves — which is also when its
 * `DisposableEffect`s run. Content that is visible on the first composition appears without an
 * entrance.
 *
 * <p>Fades by default. Unlike Compose's own `AnimatedVisibility`, which also expands by default, the
 * size is left alone unless asked for, because animating it costs a measuring wrapper.
 *
 * <p>[layout], [style] and [classes] go on the box that is being animated. The properties the
 * transitions animate are written after [style], so they win over anything it sets for the same
 * property. With [expandVertically] or [shrinkVertically] the box's height belongs to the animation —
 * give the content its height instead — and the box clips; the horizontal variants do the same for
 * width.
 */
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    content: @Composable () -> Unit,
) {
    val vertical = sizeAxis(enter.data, exit.data)
    val transition = rememberVisibilityTransition(visible, enter, exit)
    // Read here rather than inside a block, because it decides whether the content is composed at
    // all. The node observer keeps the blocks below following the animated values on its own, but it
    // can only re-run a block — it cannot mount or unmount anything.
    if (!transition.isMounted(visible)) return

    val fades = enter.data.fade != null || exit.data.fade != null
    val transforms = enter.data.slide != null || exit.data.slide != null ||
        enter.data.scale != null || exit.data.scale != null
    val written = remember { WrittenChannels() }

    // Always the same two boxes, whatever the transitions are: switching between shapes would tear
    // the content down and build it again whenever a transition gained or lost a size change.
    Box(
        layout = {
            layout?.invoke(this)
            if (vertical != null) {
                // The animated axis is the main axis, so the content inside keeps its own extent
                // along it rather than being squeezed to the shrinking box.
                flexDirection(if (vertical) FlexDirection.COLUMN else FlexDirection.ROW)
                val fraction = transition.size.value
                val extent = if (fraction >= 1f) auto else (fraction * transition.fullSize).px
                if (vertical) height(extent) else width(extent)
            }
        },
        style = {
            // A channel the transitions no longer animate is put back first — style writes are never
            // undone by themselves, so it would otherwise keep its last animated value. First, so the
            // caller's own block below can still set the same property.
            if (vertical == null && written.clip) overflowVisible(true).also { written.clip = false }
            if (!fades && written.opacity) opacity(1f).also { written.opacity = false }
            if (!transforms && written.transform) transform2D(Transform2D.identity()).also { written.transform = false }
            style?.invoke(this)
            if (vertical != null) overflowVisible(false).also { written.clip = true }
            if (fades) opacity(transition.alpha.value.coerceIn(0f, 1f)).also { written.opacity = true }
            if (transforms) {
                transform2D(
                    Transform2D.identity()
                        .translatePercent(transition.offsetX.value * 100f, transition.offsetY.value * 100f)
                        .scale(transition.scale.value.coerceAtLeast(0f))
                        .pivot(transition.pivotX, transition.pivotY),
                )
                written.transform = true
            }
        },
        classes = classes,
    ) {
        Box(
            // Explicitly never shrinking, whatever a stylesheet says: clipping only works if the
            // content keeps its own extent while the box around it does not. Growing fills the box
            // once the animation has let go of it.
            layout = if (vertical == null) null else ({ flexShrink(0); flexGrow(1) }),
            events = if (vertical == null) null else ({
                on(UIEvents.LAYOUT_CHANGED) { event ->
                    event.currentElement?.let { transition.measure(if (vertical) it.sizeHeight else it.sizeWidth) }
                }
            }),
            content = content,
        )
    }
}

/** Which of the animated style channels [AnimatedVisibility] has written, so it can put them back. */
private class WrittenChannels {
    var clip = false
    var opacity = false
    var transform = false
}
