package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.math.interpolate.Eases
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Animations, stepped frame by frame.
 *
 * <p>Being able to test these at all is a consequence of owning the frame clock: the harness decides
 * what time it is, so "150ms into a 200ms tween" is an exact statement rather than a sleep and a
 * tolerance. Nothing here waits on wall-clock time.
 */
class ComposeAnimationTest {

    /** The animated value, read back out of the node it was rendered into. */
    private fun TestHarness.value(): Float = container().name.toFloat()

    @Test
    fun `a tween starts at the old value and settles exactly on the target`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        h.setContent {
            val animated by animateFloatAsState(target, TweenSpec(durationMillis = 100))
            Node("%.4f".format(animated))
        }
        assertEquals(0f, h.value())

        target = 100f
        h.frame(0) // let the effect launch without advancing time
        assertEquals(0f, h.value(), 0.01f)

        h.advance(50)
        val midway = h.value()
        assertTrue(midway > 20f && midway < 80f, "halfway through the tween the value was $midway")

        h.advance(100)
        // Lands on the target exactly, not merely close to it - a UI that settles at 99.7% opacity
        // looks wrong forever.
        assertEquals(100f, h.value())
        h.dispose()
    }

    @Test
    fun `easing changes the path but not the endpoints`() {
        var linearTarget by mutableStateOf(0f)
        val linear = TestHarness().apply {
            setContent {
                val v by animateFloatAsState(linearTarget, TweenSpec(100, easing = Eases.LINEAR))
                Node("%.4f".format(v))
            }
        }
        var easedTarget by mutableStateOf(0f)
        val eased = TestHarness().apply {
            setContent {
                val v by animateFloatAsState(easedTarget, TweenSpec(100, easing = Eases.QUAD_IN))
                Node("%.4f".format(v))
            }
        }

        linearTarget = 100f
        easedTarget = 100f
        linear.frame(0)
        eased.frame(0)
        linear.advance(50)
        eased.advance(50)

        // QUAD_IN is t², so halfway through it has covered a quarter of the distance, not half.
        assertTrue(
            eased.value() < linear.value() - 10f,
            "linear=${linear.value()} quadIn=${eased.value()}",
        )

        linear.advance(100)
        eased.advance(100)
        assertEquals(100f, linear.value())
        assertEquals(100f, eased.value())
        linear.dispose()
        eased.dispose()
    }

    @Test
    fun `a delay holds the start value before the tween begins`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        h.setContent {
            val v by animateFloatAsState(target, TweenSpec(durationMillis = 100, delayMillis = 100))
            Node("%.4f".format(v))
        }

        target = 100f
        h.frame(0)
        h.advance(80)
        assertEquals(0f, h.value(), 0.01f)

        h.advance(120)
        assertTrue(h.value() > 0f, "the tween should have started by now")

        h.advance(100)
        assertEquals(100f, h.value())
        h.dispose()
    }

    @Test
    fun `retargeting mid-flight continues from where it is`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        h.setContent {
            val v by animateFloatAsState(target, TweenSpec(durationMillis = 100))
            Node("%.4f".format(v))
        }

        target = 100f
        h.frame(0)
        h.advance(50)
        val interrupted = h.value()
        assertTrue(interrupted > 20f && interrupted < 80f, "expected mid-flight, was $interrupted")

        // Retarget while still moving. The new tween has to pick up from the current value; starting
        // over from the old origin is what makes an interrupted animation visibly jump.
        target = 200f
        h.frame(0)
        assertTrue(h.value() >= interrupted - 1f, "snapped backwards: $interrupted -> ${h.value()}")

        h.advance(150)
        assertEquals(200f, h.value())
        h.dispose()
    }

    @Test
    fun `the finished listener fires once, with the target`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        val finished = mutableListOf<Float>()
        h.setContent {
            val v by animateFloatAsState(target, TweenSpec(100), finishedListener = { finished += it })
            Node("%.4f".format(v))
        }
        // The initial composition is already at its target, so it reports as soon as effects run —
        // which is on the first frame, not during composition.
        h.frame(0)
        assertEquals(listOf(0f), finished)

        target = 100f
        h.frame(0)
        h.advance(50)
        assertEquals(listOf(0f), finished, "should not report before it arrives")

        h.advance(100)
        assertEquals(listOf(0f, 100f), finished)

        h.advance(200)
        assertEquals(listOf(0f, 100f), finished, "should not report again once settled")
        h.dispose()
    }

    @Test
    fun `int animation rounds rather than truncating`() {
        val h = TestHarness()
        var target by mutableStateOf(0)
        h.setContent {
            val v by animateIntAsState(target, TweenSpec(100))
            Node(v.toString())
        }

        target = 10
        h.frame(0)
        h.advance(50)
        val midway = h.container().name.toInt()
        assertTrue(midway in 1..9, "expected to be partway, was $midway")

        h.advance(100)
        assertEquals("10", h.container().name)
        h.dispose()
    }

    @Test
    fun `colour animation lands exactly on its endpoints`() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        // Whatever colour space the blend uses, arriving is not negotiable.
        assertEquals(black, lerpArgb(black, white, 0f))
        assertEquals(white, lerpArgb(black, white, 1f))
        // And an out-of-range fraction is clamped rather than extrapolated past the target.
        assertEquals(black, lerpArgb(black, white, -0.5f))
        assertEquals(white, lerpArgb(black, white, 2f))
    }

    @Test
    fun `colour animation moves monotonically between the two colours`() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        // Deliberately not asserting a midpoint value: the blend goes through Oklab, so the halfway
        // colour is perceptually mid-grey rather than 0x808080. What has to hold is that it only
        // ever moves towards the target.
        var previous = -1
        for (step in 0..10) {
            val luminance = lerpArgb(black, white, step / 10f) and 0xFF
            assertTrue(luminance >= previous, "went backwards at step $step: $luminance < $previous")
            previous = luminance
        }
    }

    @Test
    fun `colour animation carries alpha across`() {
        val transparentRed = 0x00FF0000
        val opaqueRed = 0xFFFF0000.toInt()
        val mid = lerpArgb(transparentRed, opaqueRed, 0.5f)
        val alpha = (mid ushr 24) and 0xFF
        assertTrue(alpha in 1..254, "alpha should be part-way, was $alpha")
    }

    @Test
    fun `a zero duration tween arrives immediately`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        h.setContent {
            val v by animateFloatAsState(target, TweenSpec(durationMillis = 0))
            Node("%.4f".format(v))
        }

        target = 42f
        // Three hops even for an instant tween, and none of them need time to pass: one frame to
        // launch the effect, one for its first `withFrameNanos` to resume and write the value, one
        // for the reader to recompose.
        h.frame(0)
        h.frame(0)
        h.frame(0)
        assertEquals(42f, h.value())
        h.dispose()
    }

    // region springs

    @Test
    fun `a critically damped spring settles on the target without overshooting`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        val samples = mutableListOf<Float>()
        h.setContent {
            val v by animateFloatAsState(target, spring(dampingRatio = 1f, stiffness = 300f))
            samples += v
            Node("%.4f".format(v))
        }

        target = 100f
        h.frame(0)
        h.advance(1000)

        assertEquals(100f, h.value())
        assertTrue(samples.none { it > 100.01f }, "overshot: max was ${samples.max()}")
        assertTrue(samples.any { it in 1f..99f }, "jumped straight there instead of animating")
        h.dispose()
    }

    @Test
    fun `an underdamped spring overshoots and comes back`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        val samples = mutableListOf<Float>()
        h.setContent {
            val v by animateFloatAsState(target, spring(dampingRatio = 0.3f, stiffness = 300f))
            samples += v
            Node("%.4f".format(v))
        }

        target = 100f
        h.frame(0)
        h.advance(2000)

        assertTrue(samples.any { it > 100f }, "a bouncy spring should overshoot; max was ${samples.max()}")
        assertEquals(100f, h.value(), "it still has to settle exactly on the target")
        h.dispose()
    }

    @Test
    fun `a stiffer spring arrives sooner`() {
        fun framesToSettle(stiffness: Float): Int {
            val h = TestHarness()
            var target by mutableStateOf(0f)
            h.setContent {
                val v by animateFloatAsState(target, spring(stiffness = stiffness))
                Node("%.4f".format(v))
            }
            target = 100f
            h.frame(0)
            var frames = 0
            while (h.value() < 99.999f && frames < 500) {
                h.frame()
                frames++
            }
            h.dispose()
            return frames
        }

        val slow = framesToSettle(80f)
        val fast = framesToSettle(1500f)
        assertTrue(fast < slow, "stiffness 1500 took $fast frames, stiffness 80 took $slow")
    }

    @Test
    fun `a spring survives a long frame instead of exploding`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        h.setContent {
            val v by animateFloatAsState(target, spring(stiffness = 4000f))
            Node("%.4f".format(v))
        }

        target = 100f
        h.frame(0)
        // A lag spike, or a window that just regained focus. Integrating a stiff spring explicitly
        // over a step that size is exactly how these go unstable and fly off to infinity.
        h.frame(2000)
        h.frame(2000)
        assertTrue(h.value().isFinite() && h.value() in -10f..200f, "went unstable: ${h.value()}")

        h.advance(2000)
        assertEquals(100f, h.value())
        h.dispose()
    }

    @Test
    fun `a redirected spring keeps its velocity instead of stalling`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        val animatable = Animatable(0f)
        h.setContent {
            LaunchedEffect(target) { animatable.animateTo(target, spring(stiffness = 200f)) }
            Node("%.4f".format(animatable.value))
        }

        target = 100f
        h.frame(0)
        h.advance(100)
        val movingValue = h.value()
        val movingVelocity = animatable.velocity
        assertTrue(movingValue > 1f && movingValue < 99f, "expected mid-flight, was $movingValue")
        assertTrue(movingVelocity > 0f, "should be moving, velocity was $movingVelocity")

        // Retarget further out. The spring must still be travelling, not restarting from rest.
        target = 200f
        h.frame(0)
        h.frame()
        assertTrue(animatable.velocity > 0f, "velocity was lost on retarget: ${animatable.velocity}")

        h.advance(3000)
        assertEquals(200f, animatable.value)
        h.dispose()
    }

    // endregion

    @Test
    fun `Animatable snapTo jumps without animating`() {
        val h = TestHarness()
        val animatable = Animatable(0f)
        h.setContent { Node("%.4f".format(animatable.value)) }

        animatable.snapTo(42f)
        h.frame()

        assertEquals(42f, h.value())
        assertEquals(0f, animatable.velocity)
        h.dispose()
    }

    @Test
    fun `Animatable runs a sequence one step at a time`() {
        val h = TestHarness()
        val animatable = Animatable(0f)
        val reached = mutableListOf<Float>()
        h.setContent {
            LaunchedEffect(Unit) {
                animatable.animateTo(50f, TweenSpec(100))
                reached += animatable.value
                animatable.animateTo(10f, TweenSpec(100))
                reached += animatable.value
            }
            Node("%.4f".format(animatable.value))
        }

        h.advance(80)
        assertEquals(emptyList<Float>(), reached, "the first leg should still be running")

        h.advance(100)
        assertEquals(listOf(50f), reached)

        h.advance(200)
        assertEquals(listOf(50f, 10f), reached)
        assertEquals(10f, h.value())
        h.dispose()
    }
}
