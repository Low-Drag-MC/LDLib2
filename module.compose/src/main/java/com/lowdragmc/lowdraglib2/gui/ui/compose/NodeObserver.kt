package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import java.util.concurrent.ConcurrentLinkedQueue

/** Something whose props can be written to its element again, without recomposing anything. */
internal interface Reappliable {
    fun reapply()
}

/**
 * Makes a snapshot read inside a `layout` / `style` / `events` block behave the way callers expect.
 *
 * <p>The problem it solves: those blocks do not run during composition. [androidx.compose.runtime.Updater.reconcile]
 * hands them to `Composer.apply`, which defers them to the *apply* phase — the only point at which the
 * `UIElement` exists. Compose attributes state reads to the composable that is running at the time,
 * and during apply there is none, so a read from inside a block used to be attributed to nothing:
 * `style = { opacity(alpha) }` would freeze at whatever `alpha` was on the first frame, and only
 * appear to work when something else recomposed the caller for its own reasons.
 *
 * <p>The fix is the one Compose UI uses for the same shape of problem in `Modifier.drawBehind` and
 * `graphicsLayer`: run the block inside a [SnapshotStateObserver], so its reads are attributed to the
 * node, and re-run *just that node's blocks* when one of them changes. No recomposition is involved,
 * which makes it strictly cheaper than the alternative of pushing the read up into the caller.
 *
 * <p>One per [ComposeHost]; reached from a composable through [LocalNodeObserver].
 */
internal class NodeObserver {
    /**
     * Work handed over by the snapshot system, which may arrive on any thread that applies a
     * snapshot. Draining it is [flush]'s job, on the render thread, once per frame.
     */
    private val pending = ConcurrentLinkedQueue<() -> Unit>()

    /** Nodes whose observed state changed. Only ever touched while draining, so plain is fine. */
    private val invalidated = LinkedHashSet<Reappliable>()

    private val observer = SnapshotStateObserver { work -> pending.add(work) }
    private var disposed = false

    fun start() = observer.start()

    /**
     * Runs [block], recording every state it reads against [node].
     *
     * <p>The recorded set can lag by one pass. [SnapshotStateObserver] decides a read is stale by
     * comparing snapshot ids, so a block that stops reading something keeps that dependency until an
     * apply happens in a newer snapshot — which is the very next one, since a re-apply is triggered by
     * a write. The error is always in the direction of one redundant re-apply, never a missed update,
     * and a redundant re-apply writes values that `StyleBag` discards without dirtying anything.
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
     * <p>One pass, deliberately. Draining [pending] is what fills [invalidated], and that happens
     * once here, so the batch cannot grow while it is being processed by any path other than a block
     * writing the state it reads. Leaving that case for the next frame turns it into a visible
     * per-frame re-apply rather than a hang on the render thread.
     */
    fun flush() {
        while (true) {
            (pending.poll() ?: break).invoke()
        }
        if (invalidated.isEmpty()) return
        val batch = invalidated.toList()
        invalidated.clear()
        for (node in batch) node.reapply()
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

/**
 * The observer for the composition a node belongs to.
 *
 * <p>Static, because it never changes for the life of a composition — a changing value would
 * invalidate every reader. Null only for a composition built without a [ComposeHost], where blocks
 * still run, just without the read tracking.
 */
internal val LocalNodeObserver = staticCompositionLocalOf<NodeObserver?> { null }
