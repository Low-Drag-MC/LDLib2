package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import com.lowdragmc.lowdraglib2.LDLib2
import java.util.concurrent.ConcurrentLinkedQueue

/** Something whose props can be written to its element again, without recomposing anything. */
internal interface Reappliable {
    fun reapply()
}

/**
 * Tracks the state read by `layout` / `style` / `events` blocks and re-runs just that node's blocks
 * when it changes. The blocks run in the apply phase, where Compose attributes reads to no composable,
 * so without this `style = { opacity(alpha) }` would freeze at the first frame's `alpha`.
 *
 * <p>One per [ComposeHost]; reached from a composable through [LocalNodeObserver].
 */
internal class NodeObserver {
    /** Filled from whichever thread applies a snapshot; drained by [flush] on the render thread. */
    private val pending = ConcurrentLinkedQueue<() -> Unit>()

    /** Only touched while draining, so not concurrent. */
    private val invalidated = LinkedHashSet<Reappliable>()

    private val observer = SnapshotStateObserver { work -> pending.add(work) }
    private var disposed = false

    fun start() = observer.start()

    /**
     * Runs [block], recording every state it reads against [node].
     *
     * <p>A dependency the block stops reading lingers for one more pass, so the worst case is one
     * redundant re-apply (whose unchanged writes `StyleBag` discards), never a missed update.
     */
    fun observe(node: Reappliable, block: () -> Unit) {
        if (disposed) {
            block()
            return
        }
        observer.observeReads(node, onChanged, block)
    }

    private val onChanged: (Reappliable) -> Unit = { invalidated.add(it) }

    /**
     * Re-applies every node whose observed state changed since the last frame.
     *
     * <p>One pass, deliberately: a block that writes the state it reads is re-applied once per frame
     * rather than hanging the render thread.
     */
    fun flush() {
        while (true) {
            (pending.poll() ?: break).invoke()
        }
        if (invalidated.isEmpty()) return
        val batch = invalidated.toList()
        invalidated.clear()
        for (node in batch) {
            // User blocks: one that throws must not take the rest of the batch, or the frame, with it.
            try {
                node.reapply()
            } catch (e: Throwable) {
                LDLib2.LOGGER.error("A Compose layout/style block threw while re-applying", e)
            }
        }
    }

    /** Drops a node's recorded reads. Called when its slot leaves the composition. */
    fun forget(node: Reappliable) {
        invalidated.remove(node)
        observer.clear(node)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        observer.stop()
        observer.clear()
        pending.clear()
        invalidated.clear()
    }
}

/** Null for a composition built without a [ComposeHost], whose blocks run without read tracking. */
internal val LocalNodeObserver = staticCompositionLocalOf<NodeObserver?> { null }
