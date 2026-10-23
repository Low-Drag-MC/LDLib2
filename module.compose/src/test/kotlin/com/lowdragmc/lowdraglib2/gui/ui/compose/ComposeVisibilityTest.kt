package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** The state machine behind [AnimatedVisibility], stepped frame by frame. */
class ComposeVisibilityTest {

    private val spec = TweenSpec(durationMillis = 100)

    private class Subject(visible: Boolean, val enter: EnterTransition, val exit: ExitTransition) {
        val h = TestHarness()
        var visible by mutableStateOf(visible)
        val disposals = mutableListOf<String>()
        lateinit var transition: VisibilityTransition

        init {
            h.setContent {
                transition = rememberVisibilityTransition(this.visible, enter, exit)
                if (transition.isMounted(this.visible)) {
                    DisposableEffect(Unit) { onDispose { disposals += "gone" } }
                    Node("content")
                }
            }
        }

        val mounted: Boolean get() = h.tree() == "root(content)"
    }

    @Test
    fun `content visible from the start appears without an entrance`() {
        val s = Subject(true, fadeIn(spec) + slideInHorizontally(spec, -1f), fadeOut(spec))
        assertTrue(s.mounted)
        assertEquals(1f, s.transition.alpha.value)
        assertEquals(0f, s.transition.offsetX.value)
        s.h.advance(50)
        assertEquals(1f, s.transition.alpha.value)
        s.h.dispose()
    }

    @Test
    fun `hidden content rests where the entrance begins`() {
        val s = Subject(
            false,
            fadeIn(spec, initialAlpha = 0.2f) + slideInHorizontally(spec, -1f) + expandVertically(spec),
            fadeOut(spec),
        )
        assertFalse(s.mounted)
        assertEquals(0.2f, s.transition.alpha.value)
        assertEquals(-1f, s.transition.offsetX.value)
        assertEquals(0f, s.transition.size.value)
        s.h.dispose()
    }

    @Test
    fun `showing mounts at once and brings every part to its visible value`() {
        val s = Subject(false, fadeIn(spec) + slideInHorizontally(spec, -1f), fadeOut(spec))
        s.visible = true
        s.h.frame(0)
        assertTrue(s.mounted, "the content has to be in the tree from the first frame of the entrance")

        s.h.advance(50)
        val offset = s.transition.offsetX.value
        val alpha = s.transition.alpha.value
        assertTrue(offset > -1f && offset < 0f, "halfway in, the offset was $offset")
        assertTrue(alpha > 0f && alpha < 1f, "halfway in, the alpha was $alpha")

        s.h.advance(100)
        assertEquals(0f, s.transition.offsetX.value)
        assertEquals(1f, s.transition.alpha.value)
        s.h.dispose()
    }

    @Test
    fun `hiding keeps the content until the exit is over, then rests at the entrance again`() {
        val s = Subject(true, slideInHorizontally(spec, -1f), slideOutHorizontally(spec, 0.5f))
        s.visible = false
        s.h.frame(0)
        s.h.advance(50)
        assertTrue(s.mounted, "removed before the exit finished")
        assertEquals(emptyList<String>(), s.disposals)
        val offset = s.transition.offsetX.value
        assertTrue(offset > 0f && offset < 0.5f, "halfway out, the offset was $offset")

        s.h.advance(100)
        assertFalse(s.mounted)
        assertEquals(listOf("gone"), s.disposals)
        assertEquals(-1f, s.transition.offsetX.value)
        s.h.dispose()
    }

    @Test
    fun `reversing part-way continues from where the value is`() {
        val s = Subject(true, slideInHorizontally(spec, -1f), slideOutHorizontally(spec, 1f))
        s.visible = false
        s.h.frame(0)
        s.h.advance(50)
        val reached = s.transition.offsetX.value

        s.visible = true
        s.h.frame(0)
        assertEquals(reached, s.transition.offsetX.value, 0.05f)
        assertTrue(s.mounted)

        s.h.advance(200)
        assertEquals(0f, s.transition.offsetX.value)
        assertTrue(s.mounted)
        s.h.dispose()
    }

    @Test
    fun `an exit with nothing to animate removes the content straight away`() {
        val s = Subject(true, EnterTransition.None, ExitTransition.None)
        s.visible = false
        s.h.frame()
        s.h.frame()
        assertFalse(s.mounted)
        s.h.dispose()
    }

    @Test
    fun `a property only the entrance animates goes back on the entrance's spec`() {
        // An interrupted fade-in, then an exit with no fade of its own: alpha must still end at rest.
        val s = Subject(false, fadeIn(spec) + expandVertically(spec), shrinkVertically(spec))
        s.visible = true
        s.h.frame(0)
        s.h.advance(30)
        s.visible = false
        s.h.frame(0)
        s.h.advance(200)
        assertFalse(s.mounted)
        assertEquals(0f, s.transition.alpha.value)
        s.h.dispose()
    }

    @Test
    fun `the size opens from nothing and closes back to nothing`() {
        val s = Subject(false, expandVertically(spec), shrinkVertically(spec))
        s.visible = true
        s.h.frame(0)
        s.h.advance(50)
        val size = s.transition.size.value
        assertTrue(size > 0f && size < 1f, "halfway open, the size was $size")
        s.h.advance(100)
        assertEquals(1f, s.transition.size.value)

        s.visible = false
        s.h.frame(0)
        s.h.advance(200)
        assertFalse(s.mounted)
        assertEquals(0f, s.transition.size.value)
        s.h.dispose()
    }

    @Test
    fun `the scale pivot follows the side that is running`() {
        val s = Subject(false, scaleIn(spec, pivotX = 0f, pivotY = 1f), scaleOut(spec, pivotX = 1f, pivotY = 0f))
        assertEquals(0f, s.transition.pivotX)
        assertEquals(1f, s.transition.pivotY)
        s.visible = true
        s.h.advance(200)
        s.visible = false
        s.h.frame(0)
        assertEquals(1f, s.transition.pivotX)
        assertEquals(0f, s.transition.pivotY)
        s.h.dispose()
    }

    @Test
    fun `where two parts animate the same thing the left one wins`() {
        val enter = fadeIn(spec, initialAlpha = 0.3f) + fadeIn(spec, initialAlpha = 0.7f)
        assertEquals(0.3f, enter.data.fade?.alpha)
        val combined = fadeIn(spec) + slideInVertically(spec) + scaleIn(spec) + expandVertically(spec)
        assertTrue(combined.data.fade != null && combined.data.slide != null)
        assertTrue(combined.data.scale != null && combined.data.size != null)
    }

    @Test
    fun `the size can change along one axis only`() {
        assertNull(sizeAxis(fadeIn().data, fadeOut().data))
        assertEquals(true, sizeAxis(expandVertically().data, ExitTransition.None.data))
        assertEquals(false, sizeAxis(EnterTransition.None.data, shrinkHorizontally().data))
        assertThrows<IllegalArgumentException> {
            sizeAxis(expandVertically().data, shrinkHorizontally().data)
        }
    }

    @Test
    fun `measuring ignores layout noise and empty sizes`() {
        val transition = VisibilityTransition(true, TransitionData())
        transition.measure(30f)
        transition.measure(30.3f)
        assertEquals(30f, transition.fullSize)
        transition.measure(0f)
        assertEquals(30f, transition.fullSize)
        transition.measure(42f)
        assertEquals(42f, transition.fullSize)
    }
}
