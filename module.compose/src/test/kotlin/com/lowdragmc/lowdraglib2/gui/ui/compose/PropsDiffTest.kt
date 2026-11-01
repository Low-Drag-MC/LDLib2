package com.lowdragmc.lowdraglib2.gui.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** How a reused element's classes and event handlers are diffed between recompositions. */
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
    fun `nothing to do when the keys are unchanged`() {
        val (off, on) = diff(listOf("a", "b"), listOf("a", "b"))
        assertEquals(emptyList<String>(), off)
        assertEquals(emptyList<String>(), on)
    }

    @Test
    fun `added keys are applied and existing ones are left alone`() {
        val (off, on) = diff(listOf("a"), listOf("a", "b"))
        assertEquals(emptyList<String>(), off)
        assertEquals(listOf("b"), on)
    }

    @Test
    fun `dropped keys are taken back off`() {
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
    fun `an emptied spec removes everything`() {
        val (off, on) = diff(listOf("a", "b", "c"), emptyList())
        assertEquals(listOf("a", "b", "c"), off)
        assertEquals(emptyList<String>(), on)
    }

    @Test
    fun `a spec built from nothing applies everything`() {
        val (off, on) = diff(emptyList(), listOf("a", "b"))
        assertEquals(emptyList<String>(), off)
        assertEquals(listOf("a", "b"), on)
    }

    @Test
    fun `reordering the same keys is a no-op`() {
        val (off, on) = diff(listOf("a", "b", "c"), listOf("c", "a", "b"))
        assertEquals(emptyList<String>(), off)
        assertEquals(emptyList<String>(), on)
    }

    @Test
    fun `keys are applied in the order given`() {
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
    fun `equal-but-not-identical keys count as unchanged`() {
        val current = listOf(StringBuilder("a").toString())
        val next = listOf(StringBuilder("a").toString())
        val (off, on) = diff(current, next)
        assertEquals(emptyList<String>(), off)
        assertEquals(emptyList<String>(), on)
    }
}
