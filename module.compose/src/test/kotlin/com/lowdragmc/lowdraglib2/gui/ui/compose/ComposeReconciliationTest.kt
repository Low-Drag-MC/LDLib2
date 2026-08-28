package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * How the tree changes when the data changes.
 *
 * <p>These are the cases the real applier has to get right against a live `UIElement` tree, where
 * getting them wrong is expensive: a node rebuilt instead of moved loses focus, restarts its
 * animations and re-runs stylesheet matching. So most assertions here check *identity*, not just the
 * resulting shape — the shape can be right while the work behind it was completely wrong.
 */
class ComposeReconciliationTest {

    /** Renders [items] as a keyed list under one container. */
    private fun TestHarness.keyedList(items: () -> List<String>) = setContent {
        Node("list") {
            items().forEach { item -> key(item) { Node(item) } }
        }
    }

    // region insertion

    @Test
    fun `inserts at the front, middle and end`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "c"))
        h.keyedList { items }
        assertEquals("root(list(a,c))", h.tree())

        items = listOf("start", "a", "c")
        h.frame()
        assertEquals("root(list(start,a,c))", h.tree())

        items = listOf("start", "a", "b", "c")
        h.frame()
        assertEquals("root(list(start,a,b,c))", h.tree())

        items = listOf("start", "a", "b", "c", "end")
        h.frame()
        assertEquals("root(list(start,a,b,c,end))", h.tree())
        h.dispose()
    }

    @Test
    fun `appending one item costs exactly one insert and nothing else`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b", "c"))
        h.keyedList { items }

        h.applier.resetCounts()
        items = items + "d"
        h.frame()

        assertEquals(1, h.applier.inserts)
        assertEquals(0, h.applier.removes)
        assertEquals(0, h.applier.moves)
        h.dispose()
    }

    @Test
    fun `inserting in the middle does not rebuild the tail`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b", "c"))
        h.keyedList { items }
        val list = h.container()
        val b = list.children[1]
        val c = list.children[2]

        h.applier.resetCounts()
        items = listOf("a", "x", "b", "c")
        h.frame()

        assertEquals("root(list(a,x,b,c))", h.tree())
        assertEquals(1, h.applier.inserts)
        assertEquals(0, h.applier.removes)
        assertSame(b, list.children[2])
        assertSame(c, list.children[3])
        h.dispose()
    }

    // endregion

    // region removal

    @Test
    fun `removes from the front, middle and end`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b", "c", "d", "e"))
        h.keyedList { items }

        items = listOf("b", "c", "d", "e")
        h.frame()
        assertEquals("root(list(b,c,d,e))", h.tree())

        items = listOf("b", "d", "e")
        h.frame()
        assertEquals("root(list(b,d,e))", h.tree())

        items = listOf("b", "d")
        h.frame()
        assertEquals("root(list(b,d))", h.tree())
        h.dispose()
    }

    @Test
    fun `emptying and refilling a list works and produces fresh nodes`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b"))
        h.keyedList { items }
        val originalA = h.container().children[0]

        items = emptyList()
        h.frame()
        assertEquals("root(list)", h.tree())
        assertTrue(h.container().children.isEmpty())

        items = listOf("a", "b")
        h.frame()
        assertEquals("root(list(a,b))", h.tree())
        // The old subtree was disposed, so "a" cannot be the same instance it was before.
        assertNotSame(originalA, h.container().children[0])
        h.dispose()
    }

    @Test
    fun `an insert and a removal in the same frame both land`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b", "c"))
        h.keyedList { items }
        val c = h.container().children[2]

        items = listOf("a", "c", "d") // b removed, d added, c kept
        h.frame()

        assertEquals("root(list(a,c,d))", h.tree())
        assertSame(c, h.container().children[1])
        h.dispose()
    }

    // endregion

    // region movement

    @Test
    fun `swapping two adjacent items keeps both instances`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b"))
        h.keyedList { items }
        val list = h.container()
        val a = list.children[0]
        val b = list.children[1]

        h.applier.resetCounts()
        items = listOf("b", "a")
        h.frame()

        assertEquals("root(list(b,a))", h.tree())
        assertSame(b, list.children[0])
        assertSame(a, list.children[1])
        assertEquals(0, h.applier.inserts)
        assertEquals(0, h.applier.removes)
        h.dispose()
    }

    @Test
    fun `reversing a long list moves every node instead of rebuilding any`() {
        val h = TestHarness()
        var items by mutableStateOf((1..8).map { "i$it" })
        h.keyedList { items }
        val list = h.container()
        val before = list.children.toList()

        h.applier.resetCounts()
        items = items.reversed()
        h.frame()

        assertEquals("root(list(${items.joinToString(",")}))", h.tree())
        assertEquals(0, h.applier.inserts)
        assertEquals(0, h.applier.removes)
        before.reversed().forEachIndexed { i, node -> assertSame(node, list.children[i]) }
        h.dispose()
    }

    @Test
    fun `rotating a list keeps every instance`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b", "c", "d"))
        h.keyedList { items }
        val list = h.container()
        val byName = list.children.associateBy { it.name }

        items = listOf("d", "a", "b", "c")
        h.frame()

        assertEquals("root(list(d,a,b,c))", h.tree())
        list.children.forEach { assertSame(byName[it.name], it) }
        h.dispose()
    }

    @Test
    fun `a move combined with an insert and a removal resolves correctly`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b", "c", "d"))
        h.keyedList { items }
        val list = h.container()
        val a = list.children[0]
        val d = list.children[3]

        // b dropped, order shuffled, e added - all in one pass.
        items = listOf("d", "e", "a", "c")
        h.frame()

        assertEquals("root(list(d,e,a,c))", h.tree())
        assertSame(d, list.children[0])
        assertSame(a, list.children[2])
        h.dispose()
    }

    // endregion

    // region what keys are actually for

    @Composable
    private fun StatefulRow(label: String, onCreate: (String) -> Unit) {
        // Captured once per slot. If the slot is reused for a different item, this value proves it.
        val birthMark = remember { "$label@${nextMark++}" }
        remember(birthMark) { onCreate(birthMark); birthMark }
        Node(birthMark)
    }

    private var nextMark = 0

    @Test
    fun `state inside a keyed item travels with the item when the list reorders`() {
        val h = TestHarness()
        nextMark = 0
        var items by mutableStateOf(listOf("a", "b", "c"))
        h.setContent {
            Node("list") {
                items.forEach { item -> key(item) { StatefulRow(item) {} } }
            }
        }
        assertEquals("root(list(a@0,b@1,c@2))", h.tree())

        items = listOf("c", "a", "b")
        h.frame()

        // Each item kept the state it was born with; nothing was re-created.
        assertEquals("root(list(c@2,a@0,b@1))", h.tree())
        h.dispose()
    }

    @Test
    fun `without a key, state stays at the position and the item inherits the wrong one`() {
        val h = TestHarness()
        nextMark = 0
        var items by mutableStateOf(listOf("a", "b", "c"))
        h.setContent {
            Node("list") {
                // No key(): every iteration shares one call site, so slots match up by position.
                items.forEach { item -> StatefulRow(item) {} }
            }
        }
        assertEquals("root(list(a@0,b@1,c@2))", h.tree())

        items = listOf("c", "a", "b")
        h.frame()

        // The tree is still structurally fine, but slot 0's state did not follow "a" - this is
        // exactly the bug `key` exists to prevent, and why the demo screen uses it.
        assertEquals("root(list(a@0,b@1,c@2))", h.tree())
        h.dispose()
    }

    @Test
    fun `changing an item's key replaces its node`() {
        val h = TestHarness()
        var id by mutableStateOf("v1")
        h.setContent {
            Node("box") {
                key(id) { Node("node-$id") }
            }
        }
        val first = h.container().children.single()

        id = "v2"
        h.frame()

        val second = h.container().children.single()
        assertEquals("root(box(node-v2))", h.tree())
        assertNotSame(first, second)
        h.dispose()
    }

    // endregion

    // region conditionals and nesting

    @Test
    fun `an if-else swap replaces the subtree`() {
        val h = TestHarness()
        var showFirst by mutableStateOf(true)
        h.setContent {
            Node("box") {
                if (showFirst) Node("first") else Node("second")
            }
        }
        assertEquals("root(box(first))", h.tree())

        showFirst = false
        h.frame()
        assertEquals("root(box(second))", h.tree())

        showFirst = true
        h.frame()
        assertEquals("root(box(first))", h.tree())
        h.dispose()
    }

    @Test
    fun `a conditional between two static siblings lands in the right slot`() {
        val h = TestHarness()
        var middle by mutableStateOf(false)
        h.setContent {
            Node("box") {
                Node("head")
                if (middle) Node("middle")
                Node("tail")
            }
        }
        assertEquals("root(box(head,tail))", h.tree())

        middle = true
        h.frame()
        assertEquals("root(box(head,middle,tail))", h.tree())

        middle = false
        h.frame()
        assertEquals("root(box(head,tail))", h.tree())
        h.dispose()
    }

    @Test
    fun `nested keyed lists reorder independently`() {
        val h = TestHarness()
        var groups by mutableStateOf(listOf("g1" to listOf("a", "b"), "g2" to listOf("x", "y")))
        h.setContent {
            Node("root-content") {
                groups.forEach { (group, rows) ->
                    key(group) {
                        Node(group) {
                            rows.forEach { row -> key(row) { Node(row) } }
                        }
                    }
                }
            }
        }
        assertEquals("root(root-content(g1(a,b),g2(x,y)))", h.tree())

        val outer = h.container()
        val g1 = outer.children[0]
        val a = g1.children[0]

        // Reorder the outer list and one inner list at once.
        groups = listOf("g2" to listOf("y", "x"), "g1" to listOf("a", "b"))
        h.frame()

        assertEquals("root(root-content(g2(y,x),g1(a,b)))", h.tree())
        assertSame(g1, outer.children[1])
        assertSame(a, g1.children[0])
        h.dispose()
    }

    @Test
    fun `a recursive composable builds an arbitrarily deep tree`() {
        val h = TestHarness()
        var depth by mutableStateOf(3)
        h.setContent { Chain(depth) }

        assertEquals("root(n3(n2(n1)))", h.tree())

        depth = 5
        h.frame()
        assertEquals("root(n5(n4(n3(n2(n1)))))", h.tree())

        depth = 1
        h.frame()
        assertEquals("root(n1)", h.tree())
        h.dispose()
    }

    @Composable
    private fun Chain(depth: Int) {
        if (depth <= 0) return
        Node("n$depth") { Chain(depth - 1) }
    }

    @Test
    fun `sibling containers do not disturb each other`() {
        val h = TestHarness()
        var left by mutableStateOf(listOf("a"))
        var right by mutableStateOf(listOf("x"))
        h.setContent {
            Node("root-content") {
                Node("left") { left.forEach { key(it) { Node(it) } } }
                Node("right") { right.forEach { key(it) { Node(it) } } }
            }
        }
        assertEquals("root(root-content(left(a),right(x)))", h.tree())

        val rightNode = h.container().children[1]
        val x = rightNode.children[0]

        left = listOf("a", "b", "c")
        h.frame()

        assertEquals("root(root-content(left(a,b,c),right(x)))", h.tree())
        assertSame(x, rightNode.children[0])
        h.dispose()
    }

    @Test
    fun `moving an item between two lists creates a new node rather than transplanting one`() {
        val h = TestHarness()
        var left by mutableStateOf(listOf("a", "b"))
        var right by mutableStateOf(listOf<String>())
        h.setContent {
            Node("root-content") {
                Node("left") { left.forEach { key(it) { Node(it) } } }
                Node("right") { right.forEach { key(it) { Node(it) } } }
            }
        }
        val b = h.container().children[0].children[1]

        left = listOf("a")
        right = listOf("b")
        h.frame()

        assertEquals("root(root-content(left(a),right(b)))", h.tree())
        // Keys scope to their parent group; crossing parents is a remove plus an insert. Worth
        // knowing, because it means drag-and-drop between containers loses per-item state unless
        // that state is hoisted above both.
        assertNotSame(b, h.container().children[1].children[0])
        h.dispose()
    }

    @Test
    fun `movableContentOf carries its state across parents`() {
        val h = TestHarness()
        var inLeft by mutableStateOf(true)
        val created = mutableListOf<String>()
        val movable = movableContentOf {
            val id = remember { "made-${created.size}".also { created += it } }
            Node(id)
        }

        h.setContent {
            Node("root-content") {
                Node("left") { if (inLeft) movable() }
                Node("right") { if (!inLeft) movable() }
            }
        }
        assertEquals("root(root-content(left(made-0),right))", h.tree())

        inLeft = false
        h.frame()

        // The deliberate exception to the previous test: normally crossing parents is a teardown
        // plus a rebuild, and `remember` state does not survive it. This is the opt-in that moves
        // the slot table entry instead - the composable ran once, not twice.
        assertEquals("root(root-content(left,right(made-0)))", h.tree())
        assertEquals(listOf("made-0"), created)
        h.dispose()
    }

    // endregion

    @Test
    fun `several state writes in one frame produce a single settled tree`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a"))
        var title by mutableStateOf("t0")
        h.setContent {
            Node("root-content") {
                Node(title)
                Node("list") { items.forEach { key(it) { Node(it) } } }
            }
        }

        items = listOf("a", "b")
        title = "t1"
        items = listOf("a", "b", "c")
        title = "t2"
        h.frame()

        assertEquals("root(root-content(t2,list(a,b,c)))", h.tree())
        // And it converged in that one frame - no further work is pending.
        assertEquals(1, h.settle())
        h.dispose()
    }
}
