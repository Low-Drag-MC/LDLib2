package com.lowdragmc.lowdraglib2.gui.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Regression tests for the one piece of arithmetic in the applier that is easy to get wrong.
 *
 * <p>Composed child indices count only the children a composition owns; the real child list also
 * holds whatever internal parts the widget built for itself. Getting the mapping wrong is not a
 * crash — it silently inserts content in the wrong place, e.g. before a button's own label.
 */
class ComposedChildIndexTest {

    private fun flags(vararg internal: Boolean) = internal.toList()

    @Test
    fun `maps straight through when there are no internal children`() {
        val list = flags(false, false, false)
        assertEquals(0, realIndexOfComposed(list, 0))
        assertEquals(1, realIndexOfComposed(list, 1))
        assertEquals(2, realIndexOfComposed(list, 2))
    }

    @Test
    fun `skips a leading internal child`() {
        // What a Button looks like: its own TextElement first, then composed content.
        val list = flags(true, false, false)
        assertEquals(1, realIndexOfComposed(list, 0))
        assertEquals(2, realIndexOfComposed(list, 1))
    }

    @Test
    fun `skips internal children wherever they sit`() {
        val list = flags(false, true, false, true, false)
        assertEquals(0, realIndexOfComposed(list, 0))
        assertEquals(2, realIndexOfComposed(list, 1))
        assertEquals(4, realIndexOfComposed(list, 2))
    }

    @Test
    fun `an index past the end appends after everything`() {
        assertEquals(3, realIndexOfComposed(flags(true, false, false), 2))
        assertEquals(2, realIndexOfComposed(flags(true, true), 0))
        assertEquals(0, realIndexOfComposed(emptyList(), 0))
    }

    @Test
    fun `an all-internal list has no composed positions to land on`() {
        val list = flags(true, true, true)
        // Nothing to match, so every composed index appends.
        assertEquals(3, realIndexOfComposed(list, 0))
        assertEquals(3, realIndexOfComposed(list, 1))
    }
}
