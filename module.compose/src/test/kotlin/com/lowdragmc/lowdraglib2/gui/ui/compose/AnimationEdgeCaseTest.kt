package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.math.interpolate.IEase
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test
import kotlin.math.abs

/** The interruptions, extremes and odd specs the happy-path animation tests do not reach. */
class AnimationEdgeCaseTest {

    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val green = 0xFF00FF00.toInt()

    /** Largest per-channel difference between two colours. */
    private fun distance(a: Int, b: Int): Int =
        (0..24 step 8).maxOf { shift -> abs(((a shr shift) and 0xFF) - ((b shr shift) and 0xFF)) }

    @Test
    fun `a colour blends towards its target and retargets without jumping`() {
        val h = TestHarness()
        var target by mutableStateOf(red)
        var shown = 0
        h.setContent { shown = animateColorAsState(target, tween(100)).value }
        assertEquals(red, shown)

        target = blue
        h.frame(0)
        h.advance(50)
        assertTrue(shown != red && shown != blue, "halfway, the colour was one of the endpoints")

        val before = shown
        target = green
        h.frame(0)
        h.frame(16)
        assertTrue(distance(before, shown) < 64, "retargeting jumped from $before to $shown")

        h.advance(200)
        assertEquals(green, shown)
        h.dispose()
    }

    @Test
    fun `a colour sent back to where it started eases back`() {
        val h = TestHarness()
        var target by mutableStateOf(red)
        var shown = 0
        val finished = mutableListOf<Int>()
        h.setContent { shown = animateColorAsState(target, tween(100), finishedListener = { finished += it }).value }
        target = blue
        h.frame(0)
        h.advance(50)
        val partway = shown

        target = red
        h.frame(0)
        h.frame(16)
        assertTrue(distance(partway, shown) < 64, "returning jumped from $partway to $shown")
        h.advance(200)
        assertEquals(red, shown)
        // The interrupted run towards blue never reported.
        assertEquals(listOf(red), finished)
        h.dispose()
    }

    @Test
    fun `a second animateTo cancels the first`() {
        val h = TestHarness()
        val animatable = Animatable(0f)
        var firstCancelled = false
        h.setContent {
            LaunchedEffect(Unit) {
                launch {
                    try {
                        animatable.animateTo(100f, tween(1000))
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        firstCancelled = true
                        throw e
                    }
                }
                launch {
                    animatable.animateTo(-100f, tween(100))
                }
            }
        }
        h.advance(300)
        assertTrue(firstCancelled)
        assertEquals(-100f, animatable.value)
        h.dispose()
    }

    @Test
    fun `snapTo stops an animation in progress`() {
        val h = TestHarness()
        val animatable = Animatable(0f)
        h.setContent {
            LaunchedEffect(Unit) { animatable.animateTo(100f, tween(1000)) }
        }
        h.advance(100)
        animatable.snapTo(7f)
        h.advance(200)
        assertEquals(7f, animatable.value)
        h.dispose()
    }

    @ParameterizedTest
    @CsvSource("2.5,10000", "3,10000", "1,60000", "13,400", "0.1,800")
    fun `stiff, heavily damped and bouncy springs all settle on the target`(damping: Float, stiffness: Float) {
        val h = TestHarness()
        val animatable = Animatable(0f)
        val seen = mutableListOf<Float>()
        h.setContent {
            LaunchedEffect(Unit) { animatable.animateTo(100f, SpringSpec(damping, stiffness)) }
            seen += animatable.value
        }
        // Long enough for the heavily overdamped one, which is slow by nature rather than unstable.
        h.advance(20_000)
        assertTrue(seen.all { it.isFinite() }, "a frame drew a non-finite value")
        assertEquals(100f, animatable.value)
        h.dispose()
    }

    @Test
    fun `a zero-length tween still waits for its delay`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        var shown = 0f
        h.setContent { shown = animateFloatAsState(target, tween(0, delayMillis = 300)).value }
        target = 10f
        h.frame(0)
        h.advance(200)
        assertEquals(0f, shown)
        h.advance(200)
        assertEquals(10f, shown)
        h.dispose()
    }

    @Test
    fun `a spec that is a new instance every recomposition does not restart the animation`() {
        val h = TestHarness()
        var target by mutableStateOf(0f)
        var shown = 0f
        h.setContent {
            // A capturing lambda: a fresh IEase, and so an unequal TweenSpec, on every recomposition.
            // Reading the value here recomposes on every frame, so a restart would never get anywhere.
            val factor = 1f
            shown = animateFloatAsState(target, tween(100, easing = IEase { it * factor })).value
        }
        target = 10f
        h.frame(0)
        h.advance(200)
        assertEquals(10f, shown)
        h.dispose()
    }

    @Test
    fun `visibility toggled every frame never jumps`() {
        val h = TestHarness()
        var visible by mutableStateOf(false)
        lateinit var transition: VisibilityTransition
        h.setContent {
            transition = rememberVisibilityTransition(visible, fadeIn(tween(100)), fadeOut(tween(100)))
            if (transition.isMounted(visible)) Node("content")
        }
        var last = transition.alpha.value
        repeat(30) {
            visible = !visible
            h.frame(16)
            val now = transition.alpha.value
            assertTrue(abs(now - last) <= 0.2f, "alpha jumped from $last to $now")
            last = now
        }
        h.dispose()
    }

    @Test
    fun `an entrance changed while hidden is the one that runs next`() {
        val h = TestHarness()
        var visible by mutableStateOf(false)
        var enter by mutableStateOf(slideInHorizontally(tween(100), initialOffsetX = -1f))
        lateinit var transition: VisibilityTransition
        h.setContent {
            transition = rememberVisibilityTransition(visible, enter, ExitTransition.None)
            if (transition.isMounted(visible)) Node("content")
        }
        enter = slideInHorizontally(tween(100), initialOffsetX = 1f)
        h.frame()
        visible = true
        h.frame(0)
        h.frame(16)
        assertTrue(transition.offsetX.value > 0f, "the entrance started from ${transition.offsetX.value}")
        h.advance(200)
        assertEquals(0f, transition.offsetX.value)
        assertNotEquals(-1f, transition.offsetX.value)
        h.dispose()
    }
}
