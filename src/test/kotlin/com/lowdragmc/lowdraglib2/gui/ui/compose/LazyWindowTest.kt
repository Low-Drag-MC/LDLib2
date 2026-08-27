package com.lowdragmc.lowdraglib2.gui.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Which items a lazy container mounts.
 *
 * <p>The arithmetic behind both [LazyColumn] and [LazyRow], tested in isolation because the ends of
 * the range are where it goes wrong: an index past the list crashes, an index short of it leaves a
 * gap at the bottom of the scroll, and both only show up on a list long enough that nobody notices
 * until a user has ten thousand rows.
 */
class LazyWindowTest {

    /** 10 rows of 10px in a 50px viewport: five visible, so six rows plus overscan. */
    private fun window(
        total: Int = 10,
        itemSize: Float = 10f,
        viewport: Float = 50f,
        scroll: Float = 0f,
        overscan: Int = 1,
    ) = lazyWindow(total, itemSize, viewport, scroll, overscan)

    @Test
    fun `an empty list mounts nothing`() {
        assertTrue(window(total = 0).isEmpty())
    }

    @Test
    fun `at the top the window starts at zero`() {
        val w = window(scroll = 0f)
        assertEquals(0, w.first)
        assertTrue(w.last in 5..9, "expected a screenful plus overscan, got $w")
    }

    @Test
    fun `at the bottom the window reaches the last item and no further`() {
        val w = window(scroll = 1f)
        assertEquals(9, w.last, "the final row has to be reachable")
        assertTrue(w.first > 0, "and the top ones should have been dropped, got $w")
    }

    @Test
    fun `the window never runs past the end of the list`() {
        // Every scroll position, on a list far longer than the viewport.
        for (step in 0..100) {
            val w = lazyWindow(1000, 14f, 140f, step / 100f, 2)
            assertTrue(w.first >= 0, "negative start at $step: $w")
            assertTrue(w.last <= 999, "ran past the end at $step: $w")
            assertTrue(w.first <= w.last, "inverted range at $step: $w")
        }
    }

    @Test
    fun `a list shorter than the viewport mounts all of it`() {
        val w = lazyWindow(total = 3, itemSize = 10f, viewportSize = 200f, scrollFraction = 0f, overscan = 2)
        assertEquals(0, w.first)
        assertEquals(2, w.last)
    }

    @Test
    fun `before the first layout it still mounts something`() {
        // The viewport is zero until the element has been laid out once. Mounting nothing at that
        // point would mean the container measures as empty and never gets a viewport at all.
        val w = lazyWindow(total = 100, itemSize = 10f, viewportSize = 0f, scrollFraction = 0f, overscan = 2)
        assertTrue(!w.isEmpty(), "mounted nothing with no viewport yet")
        assertEquals(0, w.first)
    }

    @Test
    fun `overscan widens the window on both sides`() {
        val none = window(scroll = 0.5f, overscan = 0)
        val some = window(scroll = 0.5f, overscan = 2)
        assertTrue(some.first <= none.first, "overscan should start earlier: $none vs $some")
        assertTrue(some.last >= none.last, "overscan should end later: $none vs $some")
    }

    @Test
    fun `scrolling forward only moves the window forward`() {
        var previousFirst = -1
        for (step in 0..20) {
            val w = lazyWindow(500, 12f, 120f, step / 20f, 1)
            assertTrue(w.first >= previousFirst, "window went backwards at step $step: $w")
            previousFirst = w.first
        }
    }

    @Test
    fun `an out of range scroll fraction is clamped rather than trusted`() {
        // The fraction comes from a widget, so treat it as input rather than as a guarantee.
        val below = lazyWindow(100, 10f, 50f, -1f, 1)
        val above = lazyWindow(100, 10f, 50f, 2f, 1)
        assertEquals(0, below.first)
        assertTrue(above.last <= 99, "ran past the end: $above")
    }
}
