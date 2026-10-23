package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * `rememberSaveable` against the registry [ComposeHost] provides, and the store behind it.
 *
 * <p>The host itself needs a live element tree, so these build the same arrangement on the test
 * harness: a registry that accepts everything, restored from what the previous composition saved.
 * Closing a screen is "save, then dispose"; reopening is a new composition handed the saved map.
 */
class ComposeSaveableTest {

    @AfterEach
    fun forgetSavedScreens() = ComposeSavedState.clearAll()

    /** One composition with a registry, the shape ComposeHost gives every screen. */
    private class Screen(restored: Map<String, List<Any?>>?, content: @Composable () -> Unit) {
        val h = TestHarness()
        val registry = SaveableStateRegistry(restored) { true }

        init {
            h.setContent { CompositionLocalProvider(LocalSaveableStateRegistry provides registry) { content() } }
        }

        /** What ComposeElement does on the way out: save while everything is still registered. */
        fun close(): Map<String, List<Any?>> = registry.performSave().also { h.dispose() }
    }

    @Test
    fun `saveable state comes back after the screen is closed and reopened, remembered state does not`() {
        var saveable: MutableState<Int>? = null
        var plain: MutableState<Int>? = null
        val content = @Composable {
            saveable = rememberSaveable { mutableStateOf(0) }
            plain = remember { mutableStateOf(0) }
        }

        val first = Screen(null, content)
        saveable!!.value = 3
        plain!!.value = 3
        first.h.frame()
        val saved = first.close()

        val second = Screen(saved, content)
        assertEquals(3, saveable!!.value)
        assertEquals(0, plain!!.value)
        second.h.dispose()
    }

    @Test
    fun `values are kept as they are, with no Saver needed`() {
        data class Draft(val title: String, val tags: List<String>)

        var draft: MutableState<Draft>? = null
        val content = @Composable { draft = rememberSaveable { mutableStateOf(Draft("", emptyList())) } }

        val first = Screen(null, content)
        draft!!.value = Draft("plans", listOf("a", "b"))
        val second = Screen(first.close(), content)
        assertEquals(Draft("plans", listOf("a", "b")), draft!!.value)
        second.h.dispose()
    }

    @Test
    fun `an item that leaves and comes back gets its state back`() {
        // The arrangement the lazy lists use: one holder, a provider per item key.
        var shown by mutableStateOf(listOf("a", "b"))
        val counters = HashMap<String, MutableState<Int>>()
        val screen = Screen(null) {
            val holder = rememberSaveableStateHolder()
            for (item in shown) {
                key(item) {
                    holder.SaveableStateProvider(item) {
                        counters[item] = rememberSaveable { mutableStateOf(0) }
                        Node(item)
                    }
                }
            }
        }
        counters.getValue("a").value = 7
        counters.getValue("b").value = 2

        shown = listOf("b")
        screen.h.frame()
        assertEquals("root(b)", screen.h.tree())

        shown = listOf("a", "b")
        screen.h.frame()
        assertEquals(7, counters.getValue("a").value)
        assertEquals(2, counters.getValue("b").value)
        screen.h.dispose()
    }

    @Test
    fun `state held for items that are out of view is saved with the screen`() {
        var shown by mutableStateOf(listOf("a"))
        var counter: MutableState<Int>? = null
        val content = @Composable {
            val holder = rememberSaveableStateHolder()
            for (item in shown) {
                key(item) {
                    holder.SaveableStateProvider(item) {
                        val state = rememberSaveable { mutableStateOf(0) }
                        if (item == "a") counter = state
                    }
                }
            }
        }

        val first = Screen(null, content)
        counter!!.value = 5
        shown = emptyList()
        first.h.frame()
        val saved = first.close()

        shown = listOf("a")
        val second = Screen(saved, content)
        assertEquals(5, counter!!.value)
        second.h.dispose()
    }

    @Test
    fun `identical content in two subcompositions saves separately`() {
        // What a split view's two panes, or any two Subcompose calls, look like to the registry.
        // If the subcompositions' keys started from scratch, both counters would save under one key
        // and could come back swapped.
        val counters = arrayOfNulls<MutableState<Int>>(2)
        val content = @Composable {
            for (pane in 0..1) {
                key(pane) {
                    SubcomposeForTest(TestNode("pane$pane")) {
                        counters[pane] = rememberSaveable { mutableStateOf(0) }
                    }
                }
            }
        }

        val first = Screen(null, content)
        first.h.frame()
        counters[0]!!.value = 1
        counters[1]!!.value = 2
        val saved = first.close()
        assertEquals(2, saved.size, "the two panes saved under ${saved.keys}")

        val second = Screen(saved, content)
        second.h.frame()
        assertEquals(1, counters[0]!!.value)
        assertEquals(2, counters[1]!!.value)
        second.h.dispose()
    }

    @Test
    fun `the store keeps one entry per key and forgets on request`() {
        ComposeSavedState.save("screen", mapOf("k" to listOf(1)))
        assertEquals(mapOf("k" to listOf(1)), ComposeSavedState.restore("screen"))

        // Nothing saveable on the screen any more: no point keeping an empty entry around.
        ComposeSavedState.save("screen", emptyMap())
        assertNull(ComposeSavedState.restore("screen"))

        ComposeSavedState.save("a", mapOf("k" to listOf(1)))
        ComposeSavedState.save("b", mapOf("k" to listOf(2)))
        ComposeSavedState.clear("a")
        assertNull(ComposeSavedState.restore("a"))
        assertEquals(mapOf("k" to listOf(2)), ComposeSavedState.restore("b"))
        ComposeSavedState.clearAll()
        assertNull(ComposeSavedState.restore("b"))
    }
}

/** [Subcompose], minus the UIElement: a linked composition pointed at a test node. */
@Composable
private fun SubcomposeForTest(container: TestNode, content: @Composable () -> Unit) {
    val parent = rememberCompositionContext()
    val composition = remember(container, parent) { Composition(TestApplier(container), parent) }
    DisposableEffect(composition) { onDispose { composition.dispose() } }
    SideEffect { composition.setContent(content) }
}
