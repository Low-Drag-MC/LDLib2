package com.lowdragmc.lowdraglib2.gui.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The rule the diffed props rest on: an element is reused across recompositions, so a node that
 * stops asking for a class or an event handler has to actively take it back off.
 *
 * <p>Both halves have teeth. Forgetting to remove leaves classes nobody asked for stuck on the
 * element. Re-adding something that was already there would unregister and re-register event
 * listeners every frame — and since LDLib2 prepends listeners, that quietly reverses dispatch order
 * as well as churning.
 */
class PropsDiffTest {

    private fun diff(current: List<String>, next: List<String>) = diffKeys(current, next)

    @Test
    fun `a class spec splits on spaces and tolerates blanks`() {
        assertEquals(listOf("card", "primary"), parseClasses("card primary"))
        assertEquals(listOf("card"), parseClasses("card "))
        assertEquals(listOf("card", "hot"), parseClasses(" card  hot "))
        assertEquals(emptyList<String>(), parseClasses(null))
        assertEquals(emptyList<String>(), parseClasses("   "))
    }

    @Test
    fun `nothing to do when the chain is unchanged`() {
        val (off, on) = diff(listOf("a", "b"), listOf("a", "b"))
        assertEquals(emptyList<String>(), off)
        assertEquals(emptyList<String>(), on)
    }

    @Test
    fun `added ops are applied and existing ones are left alone`() {
        val (off, on) = diff(listOf("a"), listOf("a", "b"))
        assertEquals(emptyList<String>(), off)
        assertEquals(listOf("b"), on)
    }

    @Test
    fun `dropped ops are unapplied`() {
        val (off, on) = diff(listOf("a", "b"), listOf("a"))
        assertEquals(listOf("b"), off)
        assertEquals(emptyList<String>(), on)
    }

    @Test
    fun `a swap both removes and adds`() {
        val (off, on) = diff(listOf("a", "b"), listOf("a", "c"))
        assertEquals(listOf("b"), off)
        assertEquals(listOf("c"), on)
    }

    @Test
    fun `an emptied chain removes everything`() {
        val (off, on) = diff(listOf("a", "b", "c"), emptyList())
        assertEquals(listOf("a", "b", "c"), off)
        assertEquals(emptyList<String>(), on)
    }

    @Test
    fun `a chain built from nothing applies everything`() {
        val (off, on) = diff(emptyList(), listOf("a", "b"))
        assertEquals(emptyList<String>(), off)
        assertEquals(listOf("a", "b"), on)
    }

    @Test
    fun `reordering the same ops is a no-op`() {
        // Ops are independent property writes, so their order in the chain does not matter. Treating
        // a reorder as a change would mean pointlessly removing and re-adding every event listener.
        val (off, on) = diff(listOf("a", "b", "c"), listOf("c", "a", "b"))
        assertEquals(emptyList<String>(), off)
        assertEquals(emptyList<String>(), on)
    }

    @Test
    fun `ops are applied in chain order so a later one wins`() {
        val (_, on) = diff(emptyList(), listOf("first", "second", "third"))
        assertEquals(listOf("first", "second", "third"), on)
    }

    @Test
    fun `a wholesale replacement removes all the old before adding any of the new`() {
        val (off, on) = diff(listOf("a", "b"), listOf("c", "d"))
        assertEquals(listOf("a", "b"), off)
        assertEquals(listOf("c", "d"), on)
    }

    @Test
    fun `equal-but-not-identical ops count as unchanged`() {
        // The real ops are data classes, so two chains built from the same calls compare equal even
        // though they are different instances - this is what makes the "unchanged" path reachable.
        val current = listOf(StringBuilder("a").toString())
        val next = listOf(StringBuilder("a").toString())
        val (off, on) = diff(current, next)
        assertEquals(emptyList<String>(), off)
        assertEquals(emptyList<String>(), on)
    }
}
