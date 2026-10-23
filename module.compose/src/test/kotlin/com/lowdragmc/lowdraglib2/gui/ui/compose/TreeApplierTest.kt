package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The applier against a tree that, like the element tree, holds children the composition did not make. */
class TreeApplierTest {

    @Test
    fun `movable content keeps its children and can be edited where it went`() {
        val h = TestHarness()
        var left by mutableStateOf(true)
        var extra by mutableStateOf(false)
        h.setContent {
            val movable = remember {
                movableContentOf {
                    Node("m") {
                        Node("m1")
                        if (extra) Node("m2")
                    }
                }
            }
            Node("left") { if (left) movable() }
            Node("right") { if (!left) movable() }
        }
        left = false
        h.frame()
        assertEquals("root(left,right(m(m1)))", h.tree())
        extra = true
        h.frame()
        assertEquals("root(left,right(m(m1,m2)))", h.tree())
        extra = false
        h.frame()
        assertEquals("root(left,right(m(m1)))", h.tree())
        h.dispose()
    }

    @Test
    fun `a node taken by a parent updated first is not removed twice`() {
        val h = TestHarness()
        var top by mutableStateOf(false)
        val shared = TestNode("shared")
        h.setContent {
            Node("top") { if (top) Adopt(shared) }
            Node("bottom") {
                if (!top) Adopt(shared)
                Node("tail")
            }
        }
        top = true
        h.frame()
        assertEquals("root(top(shared),bottom(tail))", h.tree())
        top = false
        h.frame()
        assertEquals("root(top,bottom(shared,tail))", h.tree())
        h.dispose()
    }

    @Test
    fun `a remembered node emitted again starts without its old children`() {
        val h = TestHarness()
        var open by mutableStateOf(true)
        val card = TestNode("card")
        h.setContent { if (open) Adopt(card) { Node("x") } }
        open = false
        h.frame()
        open = true
        h.frame()
        assertEquals("root(card(x))", h.tree())
        h.dispose()
    }

    @Test
    fun `children added by hand keep their place and composed ones stay together`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a"))
        h.setContent { items.forEach { key(it) { Node(it) } } }
        val manual = TestNode("M")
        h.root.children.add(manual)
        manual.parent = h.root

        items = listOf("a", "b")
        h.frame()
        assertEquals("root(a,b,M)", h.tree())
        items = listOf("z", "b", "a")
        h.frame()
        assertEquals("root(z,b,a,M)", h.tree())
        items = listOf("b")
        h.frame()
        assertEquals("root(b,M)", h.tree())

        h.dispose()
        assertEquals("root(M)", h.tree())
    }
}
