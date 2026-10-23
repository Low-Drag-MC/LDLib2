package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.math.interpolate.Eases
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Animations, stepped frame by frame. */
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
        h.frame(0)
        assertEquals(0f, h.value(), 0.01f)

        h.advance(50)
        val midway = h.value()
        assertTrue(midway > 20f && midway < 80f, "halfway through the tween the value was $midway")

        h.advance(100)
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
        h.frame(0)
        assertEquals(emptyList<Float>(), finished)

        target = 100f
        h.frame(0)
        h.advance(50)
        assertEquals(emptyList<Float>(), finished, "should not report before it arrives")

        h.advance(100)
        assertEquals(listOf(100f), finished)

        h.advance(200)
        assertEquals(listOf(100f), finished, "should not report again once settled")
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
        assertEquals(black, lerpArgb(black, white, 0f))
        assertEquals(white, lerpArgb(black, white, 1f))
        assertEquals(black, lerpArgb(black, white, -0.5f))
        assertEquals(white, lerpArgb(black, white, 2f))
    }

    @Test
    fun `colour animation moves monotonically between the two colours`() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        // No midpoint assertion: the blend goes through Oklab, so halfway is not 0x808080.
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
        // Three frames: launch the effect, resume its first withFrameNanos, recompose the reader.
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
        // About 2.3 s for this spring's swing to decay under the threshold.
        h.advance(3000)

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
