package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A stand-in for the real element tree, so reconciliation can be tested without Minecraft.
 *
 * <p>`src/test` has no Minecraft on its classpath — `uitest.md` is explicit about it — which rules
 * out touching `UIElement` here. The Compose runtime is a plain JVM library though, and the applier
 * contract it drives is the same whatever the node type is. So everything about *when* Compose
 * inserts, removes and moves nodes is testable at this level; only the part that talks to a live
 * `UIElement` needs the in-client harness.
 */
internal class TestNode(var name: String) {
    val children = mutableListOf<TestNode>()

    /** Renders the subtree as `a(b,c)`, so a whole tree is one readable assertion. */
    override fun toString(): String =
        if (children.isEmpty()) name else "$name(${children.joinToString(",")})"
}

/**
 * The same shape as the real [UIApplier], minus the internal-child index mapping — that part is
 * covered separately by `ComposedChildIndexTest`, against the arithmetic in isolation.
 */
internal class TestApplier(root: TestNode) : AbstractApplier<TestNode>(root) {
    /** Counts of each operation, so tests can assert a change was minimal rather than a rebuild. */
    var inserts = 0
        private set
    var removes = 0
        private set
    var moves = 0
        private set

    fun resetCounts() {
        inserts = 0
        removes = 0
        moves = 0
    }

    override fun insertTopDown(index: Int, instance: TestNode) = Unit

    override fun insertBottomUp(index: Int, instance: TestNode) {
        inserts++
        current.children.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        removes += count
        current.children.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        moves++
        current.children.move(from, to, count)
    }

    override fun onClear() = current.children.clear()
}

@Composable
internal fun Node(name: String, content: @Composable () -> Unit = {}) {
    ComposeNode<TestNode, TestApplier>(
        factory = { TestNode(name) },
        update = { set(name) { this.name = it } },
        content = content,
    )
}

/**
 * Queues continuations instead of running them, so [TestHarness.frame] decides when they run.
 *
 * <p>Mirrors the real host: everything Compose does is funnelled onto one thread at one point in the
 * frame, which is what keeps recomposition away from Minecraft's threading rules.
 */
private class QueueDispatcher : CoroutineDispatcher() {
    private val queue = ArrayDeque<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.addLast(block)
    }

    fun drain() {
        while (queue.isNotEmpty()) queue.removeFirst().run()
    }
}

/**
 * Drives a composition one simulated frame at a time.
 *
 * <p>The frame sequence is deliberately identical to [ComposeHost.onFrame]; if the two drift, these
 * tests stop saying anything about the real thing.
 */
internal class TestHarness {
    val root = TestNode("root")
    val applier = TestApplier(root)

    private val clock = BroadcastFrameClock()
    private val dispatcher = QueueDispatcher()
    private val scope = CoroutineScope(dispatcher + clock)
    private val recomposer = Recomposer(scope.coroutineContext)
    private val composition = Composition(applier, recomposer)
    private var frameNanos = 0L

    init {
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        dispatcher.drain()
    }

    fun setContent(content: @Composable () -> Unit) {
        composition.setContent(content)
    }

    /**
     * One frame: publish snapshot writes, let the recomposer see them, then tick it.
     *
     * @param advanceMillis how far the frame clock moves. Anything reading `withFrameNanos` — which
     *   is to say anything animated — sees this as elapsed time, so a test can step an animation
     *   frame by frame instead of waiting for one.
     */
    fun frame(advanceMillis: Long = 16) {
        Snapshot.sendApplyNotifications()
        dispatcher.drain()
        frameNanos += advanceMillis * 1_000_000
        clock.sendFrame(frameNanos)
        dispatcher.drain()
    }

    /** Runs [millis] worth of frames at [stepMillis] each. */
    fun advance(millis: Long, stepMillis: Long = 16) {
        var remaining = millis
        while (remaining > 0) {
            val step = minOf(stepMillis, remaining)
            frame(step)
            remaining -= step
        }
    }

    /** Runs frames until the tree stops changing, up to [limit]. Returns the frames consumed. */
    fun settle(limit: Int = 10): Int {
        var previous = tree()
        for (i in 1..limit) {
            frame()
            val now = tree()
            if (now == previous) return i
            previous = now
        }
        return limit
    }

    /** Mirrors [ComposeHost.dispose], final drain included — see the comment there for why. */
    fun dispose() {
        composition.dispose()
        recomposer.cancel()
        scope.cancel()
        dispatcher.drain()
    }

    fun tree(): String = root.toString()

    /** Convenience for the very common single-container shape `root(container(...))`. */
    fun container(): TestNode = root.children.single()
}
