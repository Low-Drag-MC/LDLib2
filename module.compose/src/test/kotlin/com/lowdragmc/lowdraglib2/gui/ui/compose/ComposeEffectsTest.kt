package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.awaitCancellation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Effect and state semantics under our own frame driver. */
class ComposeEffectsTest {

    @Test
    fun `LaunchedEffect restarts when its key changes and not otherwise`() {
        val h = TestHarness()
        var key by mutableStateOf("k1")
        var unrelated by mutableStateOf(0)
        val log = mutableListOf<String>()

        h.setContent {
            Node("box") {
                @Suppress("UNUSED_EXPRESSION")
                unrelated
                LaunchedEffect(key) {
                    // Captured: by the time `finally` runs, `key` already holds the new value.
                    val started = key
                    log += "start:$started"
                    try {
                        awaitCancellation()
                    } finally {
                        log += "stop:$started"
                    }
                }
                Node("child")
            }
        }
        h.frame()
        assertEquals(listOf("start:k1"), log)

        unrelated = 1
        h.frame()
        assertEquals(listOf("start:k1"), log)

        key = "k2"
        h.frame()
        assertEquals(listOf("start:k1", "stop:k1", "start:k2"), log)
        h.dispose()
    }

    @Test
    fun `DisposableEffect cleans up when its key changes`() {
        val h = TestHarness()
        var key by mutableStateOf("a")
        val log = mutableListOf<String>()

        h.setContent {
            Node("box") {
                DisposableEffect(key) {
                    val acquired = key // captured: onDispose runs after the change
                    log += "acquire:$acquired"
                    onDispose { log += "release:$acquired" }
                }
                Node("child")
            }
        }
        assertEquals(listOf("acquire:a"), log)

        key = "b"
        h.frame()
        assertEquals(listOf("acquire:a", "release:a", "acquire:b"), log)

        h.dispose()
        assertEquals(listOf("acquire:a", "release:a", "acquire:b", "release:b"), log)
    }

    @Test
    fun `disposing the composition unwinds every live effect`() {
        val h = TestHarness()
        val released = mutableListOf<String>()

        h.setContent {
            Node("box") {
                listOf("a", "b", "c").forEach { id ->
                    key(id) {
                        DisposableEffect(Unit) { onDispose { released += id } }
                        Node(id)
                    }
                }
            }
        }
        assertEquals(emptyList<String>(), released)

        h.dispose()
        assertTrue(released.containsAll(listOf("a", "b", "c")), "released=$released")
    }

    @Test
    fun `SideEffect runs after every composition that actually happened`() {
        val h = TestHarness()
        var value by mutableStateOf(0)
        val seen = mutableListOf<Int>()

        h.setContent {
            Node("box") {
                SideEffect { seen += value }
                Node("v$value")
            }
        }
        assertEquals(listOf(0), seen)

        value = 1
        h.frame()
        value = 2
        h.frame()

        assertEquals(listOf(0, 1, 2), seen)
        assertEquals("root(box(v2))", h.tree())
        h.dispose()
    }

    @Test
    fun `rememberUpdatedState lets a long-lived effect see the newest callback`() {
        val h = TestHarness()
        var handler by mutableStateOf({ "first" })
        var fired: String? = null

        h.setContent {
            Node("box") {
                val current by rememberUpdatedState(handler)
                LaunchedEffect(Unit) {
                    try {
                        awaitCancellation()
                    } finally {
                        fired = current()
                    }
                }
                Node("child")
            }
        }
        h.frame()

        handler = { "second" }
        h.frame()

        h.dispose()
        assertEquals("second", fired)
    }

    @Test
    fun `effects in a removed subtree are cancelled, not left running`() {
        val h = TestHarness()
        var items by mutableStateOf(listOf("a", "b"))
        val running = mutableSetOf<String>()

        h.setContent {
            Node("list") {
                items.forEach { item ->
                    key(item) {
                        DisposableEffect(item) {
                            running += item
                            onDispose { running -= item }
                        }
                        Node(item)
                    }
                }
            }
        }
        assertEquals(setOf("a", "b"), running)

        items = listOf("b")
        h.frame()
        assertEquals(setOf("b"), running)

        items = listOf("b", "c")
        h.frame()
        assertEquals(setOf("b", "c"), running)
        h.dispose()
    }

    @Test
    fun `nested CompositionLocal providers shadow and then restore`() {
        val h = TestHarness()
        val local = compositionLocalOf { "root" }

        h.setContent {
            Node("box") {
                Node(local.current)
                CompositionLocalProvider(local provides "outer") {
                    Node(local.current)
                    CompositionLocalProvider(local provides "inner") {
                        Node(local.current)
                    }
                    Node(local.current)
                }
                Node(local.current)
            }
        }

        assertEquals("root(box(root,outer,inner,outer,root))", h.tree())
        h.dispose()
    }

    @Test
    fun `a changed CompositionLocal reaches readers nested below the provider`() {
        val h = TestHarness()
        var provided by mutableStateOf("v1")

        h.setContent {
            CompositionLocalProvider(localForReader provides provided) {
                Node("box") {
                    Reader()
                    Node("static")
                }
            }
        }
        assertEquals("root(box(read-v1,static))", h.tree())

        provided = "v2"
        h.frame()

        assertEquals("root(box(read-v2,static))", h.tree())
        h.dispose()
    }

    @Composable
    private fun Reader() {
        Node("read-${localForReader.current}")
    }

    @Test
    fun `state written from inside an effect settles on the following frame`() {
        val h = TestHarness()
        var label by mutableStateOf("initial")

        h.setContent {
            Node("box") {
                LaunchedEffect(Unit) { label = "from-effect" }
                Node(label)
            }
        }
        assertEquals("root(box(initial))", h.tree())

        // Frame one runs the effect, which writes; frame two publishes that write and recomposes.
        h.frame()
        h.frame()
        assertEquals("root(box(from-effect))", h.tree())
        h.dispose()
    }

    @Test
    fun `withFrameNanos resumes once per frame with the frame time`() {
        val h = TestHarness()
        val stamps = mutableListOf<Long>()

        h.setContent {
            Node("box") {
                LaunchedEffect(Unit) {
                    while (true) {
                        withFrameNanos { stamps += it }
                    }
                }
                Node("child")
            }
        }

        h.frame()
        h.frame()
        h.frame()

        assertEquals(3, stamps.size)
        assertTrue(stamps[1] > stamps[0] && stamps[2] > stamps[1], "stamps=$stamps")
        h.dispose()
    }

    @Test
    fun `a snapshot batches its writes into one recomposition`() {
        val h = TestHarness()
        var a by mutableStateOf(0)
        var b by mutableStateOf(0)
        var bodyRuns = 0

        h.setContent {
            Node("box") {
                bodyRuns++
                Node("$a-$b")
            }
        }
        assertEquals(1, bodyRuns)

        Snapshot.withMutableSnapshot {
            a = 1
            b = 2
        }
        h.frame()

        assertEquals("root(box(1-2))", h.tree())
        assertEquals(2, bodyRuns)
        h.dispose()
    }

    private companion object {
        val localForReader = compositionLocalOf { "none" }
    }
}
