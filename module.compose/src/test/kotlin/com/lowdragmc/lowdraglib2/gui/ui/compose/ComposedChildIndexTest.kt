package com.lowdragmc.lowdraglib2.gui.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Where the applier puts composed children among children it does not own. Upper case stands in for
 * those: a widget's internal parts, or children added by hand.
 */
class ComposedChildIndexTest {

    /** Inserts [child] at composed [index] the way [TreeApplier] does, returning the new real list. */
    private fun insert(real: List<String>, composed: List<String>, index: Int, child: String): List<String> {
        val siblings = composed.toMutableList().apply { add(index, child) }
        val at = insertionIndex(siblings, index, real.size) { real.indexOf(it) }
        return real.toMutableList().apply { add(at, child) }
    }

    @Test
    fun `the first composed child goes after the internal parts`() {
        assertEquals(listOf("A", "a"), insert(listOf("A"), emptyList(), 0, "a"))
        assertEquals(listOf("a"), insert(emptyList(), emptyList(), 0, "a"))
    }

    @Test
    fun `an append stays with the composed children, before a child added by hand later`() {
        assertEquals(listOf("a", "b", "M"), insert(listOf("a", "M"), listOf("a"), 1, "b"))
    }

    @Test
    fun `an insert goes before the next composed sibling`() {
        assertEquals(listOf("A", "a", "b", "c"), insert(listOf("A", "a", "c"), listOf("a", "c"), 1, "b"))
        assertEquals(listOf("A", "z", "a"), insert(listOf("A", "a"), listOf("a"), 0, "z"))
    }

    @Test
    fun `siblings no longer in the real list are skipped`() {
        // "b" was taken by another parent: composed still lists it, the real list does not.
        assertEquals(listOf("a", "c", "M"), insert(listOf("a", "M"), listOf("a", "b"), 2, "c"))
        assertEquals(listOf("A", "c"), insert(listOf("A"), listOf("b"), 1, "c"))
    }

    @Test
    fun `a splice moves only the picked-out children`() {
        val children = listOf("A", "b", "c", "D", "e")
        val spliced = spliceOrder(children, listOf("e", "c", "b")) { it.first().isLowerCase() }
        assertEquals(listOf("A", "e", "c", "D", "b"), spliced)
    }

    @Test
    fun `a splice refuses an order that does not match the tree`() {
        val children = listOf("a", "b", "c")
        assertEquals(null, spliceOrder(children, listOf("a", "b")) { true })
        assertEquals(null, spliceOrder(children, listOf("a", "b", "c", "d")) { true })
    }
}
