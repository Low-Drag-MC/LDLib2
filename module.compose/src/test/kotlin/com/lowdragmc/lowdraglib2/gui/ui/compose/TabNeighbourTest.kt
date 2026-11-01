package com.lowdragmc.lowdraglib2.gui.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TabNeighbourTest {

    @Test
    fun `the next surviving tab wins`() {
        assertEquals("c", neighbourOf("b", listOf("a", "b", "c"), listOf("a", "c")))
    }

    @Test
    fun `the previous one when nothing after it survives`() {
        assertEquals("b", neighbourOf("c", listOf("a", "b", "c"), listOf("a", "b")))
    }

    @Test
    fun `tabs removed in the same batch are skipped`() {
        assertEquals("a", neighbourOf("c", listOf("a", "b", "c"), listOf("a")))
        assertEquals("d", neighbourOf("a", listOf("a", "b", "c", "d"), listOf("d")))
    }

    @Test
    fun `a tab added in the same batch is not a neighbour while an old one survives`() {
        assertEquals("a", neighbourOf("b", listOf("a", "b"), listOf("x", "a")))
    }

    @Test
    fun `with no old tab left the first tab now declared is chosen`() {
        assertEquals("x", neighbourOf("a", listOf("a", "b"), listOf("x", "y")))
    }

    @Test
    fun `a key missing from the old order falls back to the first tab`() {
        assertEquals("a", neighbourOf("z", listOf("a", "b"), listOf("a", "b")))
    }

    @Test
    fun `nothing left means no selection`() {
        assertNull(neighbourOf("a", listOf("a"), emptyList()))
    }
}
