package com.lowdragmc.lowdraglib2.gui.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The arithmetic behind variable-height [LazyColumn].
 *
 * <p>Two properties matter more than any individual case, and most of these tests are really checks
 * on one of them:
 *
 * <ul>
 * <li><b>The spacers plus the mounted rows always add up to the whole list.</b> That is what keeps
 *     the scrollbar honest — get it wrong and the thumb is the wrong size, or scrolling to the bottom
 *     does not reach the last row.</li>
 * <li><b>Every row the viewport touches is mounted.</b> Miss one and there is a hole on screen.</li>
 * </ul>
 */
class MeasuredWindowTest {

    private fun heights(vararg values: Float) = values

    private fun uniform(count: Int, height: Float) = FloatArray(count) { height }

    /** leading + mounted + trailing must equal the total, whatever the window came out as. */
    private fun assertAccountsForEverything(heights: FloatArray, window: LazyWindow) {
        val total = heights.sum()
        val mounted = if (window.range.isEmpty()) 0f else {
            (window.range).fold(0f) { acc, i -> acc + heights[i] }
        }
        assertEquals(total, window.leading + mounted + window.trailing, 0.01f,
            "leading ${window.leading} + mounted $mounted + trailing ${window.trailing} != $total")
    }

    @Test
    fun `an empty list mounts nothing`() {
        val window = measuredWindow(FloatArray(0), viewportSize = 100f, scrollFraction = 0f, overscan = 2)
        assertTrue(window.range.isEmpty())
        assertEquals(0f, window.leading)
        assertEquals(0f, window.trailing)
    }

    @Test
    fun `a list shorter than the viewport mounts all of it with no spacers`() {
        val h = uniform(3, 10f)
        val window = measuredWindow(h, viewportSize = 100f, scrollFraction = 0f, overscan = 0)
        assertEquals(0..2, window.range)
        assertEquals(0f, window.leading)
        assertEquals(0f, window.trailing)
        assertAccountsForEverything(h, window)
    }

    @Test
    fun `at the top there is no leading spacer`() {
        val h = uniform(100, 10f)
        val window = measuredWindow(h, viewportSize = 50f, scrollFraction = 0f, overscan = 2)
        assertEquals(0, window.range.first)
        assertEquals(0f, window.leading)
        assertAccountsForEverything(h, window)
    }

    @Test
    fun `at the bottom the last row is mounted and there is no trailing spacer`() {
        val h = uniform(100, 10f)
        val window = measuredWindow(h, viewportSize = 50f, scrollFraction = 1f, overscan = 2)
        assertEquals(99, window.range.last)
        assertEquals(0f, window.trailing)
        assertAccountsForEverything(h, window)
    }

    @Test
    fun `uneven heights put the leading spacer at the true offset`() {
        // Rows: 20, 5, 30, 5, 20 — total 80. Viewport 20 at the very bottom starts at 60.
        val h = heights(20f, 5f, 30f, 5f, 20f)
        val window = measuredWindow(h, viewportSize = 20f, scrollFraction = 1f, overscan = 0)
        // 60px in is exactly the start of row 4, and it fills the viewport by itself.
        assertEquals(4..4, window.range)
        assertEquals(60f, window.leading)
        assertEquals(0f, window.trailing)
        assertAccountsForEverything(h, window)
    }

    @Test
    fun `a tall row in the middle is found without mounting the ones before it`() {
        val h = heights(10f, 10f, 200f, 10f, 10f)
        // Half way down a 240px list with a 40px viewport: scrolled = 100.
        val window = measuredWindow(h, viewportSize = 40f, scrollFraction = 0.5f, overscan = 0)
        assertEquals(2..2, window.range)
        assertEquals(20f, window.leading)
        assertAccountsForEverything(h, window)
    }

    @Test
    fun `every row the viewport covers is mounted`() {
        val h = heights(10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f)
        val viewport = 25f
        // scrolled = 0.5 * (100 - 25) = 37.5, so the viewport covers 37.5..62.5 -> rows 3,4,5,6
        val window = measuredWindow(h, viewportSize = viewport, scrollFraction = 0.5f, overscan = 0)
        assertTrue(3 in window.range, "row 3 missing from ${window.range}")
        assertTrue(6 in window.range, "row 6 missing from ${window.range}")
        assertAccountsForEverything(h, window)
    }

    @Test
    fun `overscan widens the window on both sides without breaking the total`() {
        val h = uniform(100, 10f)
        val none = measuredWindow(h, viewportSize = 50f, scrollFraction = 0.5f, overscan = 0)
        val some = measuredWindow(h, viewportSize = 50f, scrollFraction = 0.5f, overscan = 3)
        assertEquals(none.range.first - 3, some.range.first)
        assertEquals(none.range.last + 3, some.range.last)
        assertAccountsForEverything(h, some)
    }

    @Test
    fun `overscan is clamped at both ends of the list`() {
        val h = uniform(10, 10f)
        val top = measuredWindow(h, viewportSize = 20f, scrollFraction = 0f, overscan = 50)
        assertEquals(0..9, top.range)
        assertAccountsForEverything(h, top)

        val bottom = measuredWindow(h, viewportSize = 20f, scrollFraction = 1f, overscan = 50)
        assertEquals(0..9, bottom.range)
        assertAccountsForEverything(h, bottom)
    }

    @Test
    fun `before the first layout it still mounts something`() {
        val h = uniform(100, 10f)
        val window = measuredWindow(h, viewportSize = 0f, scrollFraction = 0f, overscan = 0)
        assertFalse(window.range.isEmpty())
        assertAccountsForEverything(h, window)
    }

    @Test
    fun `an out of range scroll fraction is clamped rather than trusted`() {
        val h = uniform(50, 10f)
        val over = measuredWindow(h, viewportSize = 40f, scrollFraction = 5f, overscan = 1)
        val under = measuredWindow(h, viewportSize = 40f, scrollFraction = -5f, overscan = 1)
        assertEquals(49, over.range.last)
        assertEquals(0, under.range.first)
        assertAccountsForEverything(h, over)
        assertAccountsForEverything(h, under)
    }

    @Test
    fun `scrolling forward only moves the window forward`() {
        val h = uniform(200, 7f)
        var previousFirst = -1
        var fraction = 0f
        while (fraction <= 1f) {
            val window = measuredWindow(h, viewportSize = 60f, scrollFraction = fraction, overscan = 1)
            assertTrue(window.range.first >= previousFirst,
                "window went backwards at $fraction: ${window.range.first} < $previousFirst")
            assertAccountsForEverything(h, window)
            previousFirst = window.range.first
            fraction += 0.05f
        }
    }

    /**
     * The mixed state this exists to handle: some rows measured, the rest still on the estimate.
     * The window has to be right for the numbers it has, and it has to keep adding up.
     */
    @Test
    fun `a half-measured list still accounts for its whole height`() {
        val h = FloatArray(100) { if (it < 20) 5f + it % 7 else 14f }
        for (fraction in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val window = measuredWindow(h, viewportSize = 45f, scrollFraction = fraction, overscan = 2)
            assertAccountsForEverything(h, window)
        }
    }

    @Test
    fun `a single row list is mounted whatever the scroll says`() {
        val h = heights(30f)
        for (fraction in listOf(0f, 0.5f, 1f)) {
            val window = measuredWindow(h, viewportSize = 10f, scrollFraction = fraction, overscan = 2)
            assertEquals(0..0, window.range)
            assertEquals(0f, window.leading)
            assertEquals(0f, window.trailing)
        }
    }
}

/**
 * The measurement cache underneath the window.
 *
 * <p>Its whole job is to be cheap, to not thrash — layout produces fractional heights, and a cache
 * that reported every wobble would invalidate the window every frame — and to keep each measurement
 * with its item when the list changes around it.
 */
class ItemSizesTest {

    @Test
    fun `items start on the estimate`() {
        val sizes = ItemSizes(12f)
        sizes.lineUp(listOf("a", "b", "c"))
        assertEquals(listOf(12f, 12f, 12f), sizes.sizes.toList())
    }

    @Test
    fun `a measurement lands at the item's position and bumps the version`() {
        val sizes = ItemSizes(12f)
        sizes.lineUp(listOf("a", "b"))
        val before = sizes.version
        sizes.measure("b", 1, 40f)
        assertEquals(listOf(12f, 40f), sizes.sizes.toList())
        assertTrue(sizes.version > before)
    }

    @Test
    fun `measurements move with their items when one before them is removed`() {
        val sizes = ItemSizes(12f)
        sizes.lineUp(listOf("a", "b", "c"))
        sizes.measure("a", 0, 30f)
        sizes.measure("c", 2, 50f)
        sizes.lineUp(listOf("b", "c"))
        assertEquals(listOf(12f, 50f), sizes.sizes.toList())
    }

    @Test
    fun `growing the list keeps what was already measured`() {
        val sizes = ItemSizes(12f)
        sizes.lineUp(listOf("a", "b"))
        sizes.measure("a", 0, 40f)
        sizes.lineUp(listOf("a", "b", "c", "d"))
        assertEquals(listOf(40f, 12f, 12f, 12f), sizes.sizes.toList())
    }

    @Test
    fun `sub-pixel wobble is ignored`() {
        val sizes = ItemSizes(12f)
        sizes.lineUp(listOf("a", "b"))
        sizes.measure("a", 0, 40f)
        val settled = sizes.version
        sizes.measure("a", 0, 40.4f)
        sizes.measure("a", 0, 39.7f)
        assertEquals(settled, sizes.version)
        assertEquals(40f, sizes.sizes[0])
    }

    @Test
    fun `a zero size is refused`() {
        val sizes = ItemSizes(12f)
        sizes.lineUp(listOf("a", "b"))
        sizes.measure("a", 0, 0f)
        assertEquals(12f, sizes.sizes[0])
    }

    @Test
    fun `an out of range position is recorded for the key without throwing`() {
        val sizes = ItemSizes(12f)
        sizes.lineUp(listOf("a", "b"))
        sizes.measure("z", 5, 40f)
        assertEquals(listOf(12f, 12f), sizes.sizes.toList())
        sizes.lineUp(listOf("a", "z"))
        assertEquals(listOf(12f, 40f), sizes.sizes.toList())
    }
}
