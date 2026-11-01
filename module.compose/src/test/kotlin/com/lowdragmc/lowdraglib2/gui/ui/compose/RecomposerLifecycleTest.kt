package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

/**
 * [ComposeHost]'s two-stage start against the real recomposer: compose before the recomposer loop
 * runs, then catch up on what changed in between with one `invalidateAll`.
 */
class RecomposerLifecycleTest {

    private class Queue : CoroutineDispatcher() {
        val blocks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            blocks.addLast(block)
        }

        fun drain() {
            while (blocks.isNotEmpty()) blocks.removeFirst().run()
        }
    }

    @Test
    fun `the initial composition builds the tree before the recomposer loop runs`() {
        val dispatcher = Queue()
        val clock = BroadcastFrameClock()
        val scope = CoroutineScope(dispatcher + clock)
        val recomposer = Recomposer(scope.coroutineContext)
        val root = TestNode("root")
        val composition = ControlledComposition(TestApplier(root), recomposer)
        var label by mutableStateOf("a")
        val effects = mutableListOf<String>()

        composition.setContent {
            Node(label)
            DisposableEffect(Unit) {
                effects += "remembered"
                onDispose { effects += "disposed" }
            }
            LaunchedEffect(Unit) { effects += "launched" }
        }
        assertEquals("root(a)", root.toString())
        assertEquals(listOf("remembered"), effects)

        // A change while nothing listens.
        label = "b"

        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        dispatcher.drain()
        composition.invalidateAll()
        Snapshot.sendApplyNotifications()
        dispatcher.drain()
        clock.sendFrame(16_000_000)
        dispatcher.drain()

        assertEquals("root(b)", root.toString())
        assertEquals(listOf("remembered", "launched"), effects)

        composition.dispose()
        recomposer.cancel()
        scope.cancel()
        dispatcher.drain()
        assertEquals(listOf("remembered", "launched", "disposed"), effects)
    }
}
