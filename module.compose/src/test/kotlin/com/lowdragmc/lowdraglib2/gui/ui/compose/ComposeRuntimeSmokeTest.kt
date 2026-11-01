package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.awaitCancellation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** The baseline: does the Compose runtime work at all, driven by our own frame clock and applier? */
class ComposeRuntimeSmokeTest {

    @Test
    fun `initial composition builds the node tree`() {
        val h = TestHarness()
        h.setContent {
            Node("column") {
                Node("a")
                Node("b")
            }
        }

        assertEquals("root(column(a,b))", h.tree())
        h.dispose()
    }

    @Test
    fun `appending to a list reuses the existing node instances`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("A", "B", "C"))
        h.setContent {
            Node("list") {
                items.forEach { item -> key(item) { Node(item) } }
            }
        }

        val list = h.container()
        val before = list.children.toList()
        assertEquals("root(list(A,B,C))", h.tree())

        items = items + "D"
        h.frame()

        assertEquals("root(list(A,B,C,D))", h.tree())
        before.forEachIndexed { i, node -> assertSame(node, list.children[i]) }
        h.dispose()
    }

    @Test
    fun `key preserves node identity across a reorder`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("A", "B", "C"))
        h.setContent {
            Node("list") {
                items.forEach { item -> key(item) { Node(item) } }
            }
        }

        val list = h.container()
        val a = list.children[0]
        val c = list.children[2]

        items = listOf("C", "A", "B")
        h.frame()

        assertEquals("root(list(C,A,B))", h.tree())
        assertSame(c, list.children[0])
        assertSame(a, list.children[1])
        h.dispose()
    }

    @Test
    fun `remember survives recomposition and is discarded with its subtree`() {
        val h = TestHarness()
        var visible by mutableStateOf(true)
        var bump by mutableStateOf(0)
        val created = mutableListOf<String>()
        val disposed = mutableListOf<String>()

        h.setContent {
            Node("root-content") {
                @Suppress("UNUSED_EXPRESSION")
                bump // read so the enclosing scope recomposes
                if (visible) {
                    val id = remember { "instance-${created.size}".also { created += it } }
                    DisposableEffect(Unit) { onDispose { disposed += id } }
                    Node(id)
                }
            }
        }

        assertEquals(listOf("instance-0"), created)

        bump++
        h.frame()
        assertEquals(listOf("instance-0"), created)
        assertEquals("root(root-content(instance-0))", h.tree())

        visible = false
        h.frame()
        assertEquals("root(root-content)", h.tree())
        assertEquals(listOf("instance-0"), disposed)

        visible = true
        h.frame()
        assertEquals(listOf("instance-0", "instance-1"), created)
        h.dispose()
    }

    @Test
    fun `node properties update in place`() {
        val h = TestHarness()
        var label by mutableStateOf("before")
        h.setContent { Node("box") { Node(label) } }

        val box = h.container()
        val child = box.children.single()
        assertEquals("root(box(before))", h.tree())

        label = "after"
        h.frame()

        assertEquals("root(box(after))", h.tree())
        assertSame(child, box.children.single())
        assertEquals(1, box.children.size)
        h.dispose()
    }

    @Test
    fun `LaunchedEffect starts on the frame clock and is cancelled with its subtree`() {
        val h = TestHarness()
        var visible by mutableStateOf(true)
        val log = mutableListOf<String>()

        h.setContent {
            Node("root-content") {
                if (visible) {
                    LaunchedEffect(Unit) {
                        log += "started"
                        try {
                            awaitCancellation()
                        } finally {
                            log += "cancelled"
                        }
                    }
                    Node("child")
                }
            }
        }

        assertEquals(emptyList<String>(), log)
        h.frame()
        assertEquals(listOf("started"), log)

        visible = false
        h.frame()
        assertEquals(listOf("started", "cancelled"), log)
        assertEquals("root(root-content)", h.tree())
        h.dispose()
    }

    private var siblingBodyRuns = 0

    /** A function of its own because skipping happens at composable boundaries. */
    @Composable
    private fun Sibling(label: String) {
        siblingBodyRuns++
        Node(label)
    }

    @Test
    fun `an unchanged sibling composable is skipped rather than recomposed`() {
        val h = TestHarness()
        var left by mutableStateOf(0)
        siblingBodyRuns = 0

        h.setContent {
            Node("root-content") {
                Node("left-$left")
                Sibling("right")
            }
        }
        assertEquals(1, siblingBodyRuns)

        left = 1
        h.frame()
        left = 2
        h.frame()

        assertEquals("root(root-content(left-2,right))", h.tree())
        assertEquals(1, siblingBodyRuns)
        h.dispose()
    }

    @Test
    fun `derivedStateOf only invalidates readers when the derived value actually changes`() {
        val h = TestHarness()
        var source by mutableStateOf(0)
        val isLarge = derivedStateOf { source > 10 }
        var readerRuns = 0

        h.setContent {
            Node("root-content") {
                readerRuns++
                Node(if (isLarge.value) "large" else "small")
            }
        }
        assertEquals(1, readerRuns)

        source = 5
        h.frame()
        assertEquals(1, readerRuns)
        assertEquals("root(root-content(small))", h.tree())

        source = 50
        h.frame()
        assertEquals(2, readerRuns)
        assertEquals("root(root-content(large))", h.tree())
        h.dispose()
    }

    @Test
    fun `CompositionLocal reaches nested composables`() {
        val h = TestHarness()
        val local = compositionLocalOf { "default" }

        h.setContent {
            Node("root-content") {
                Node(local.current)
                CompositionLocalProvider(local provides "provided") {
                    Node(local.current)
                }
            }
        }

        assertEquals("root(root-content(default,provided))", h.tree())
        h.dispose()
    }
}
