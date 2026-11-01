package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/** A stand-in for the real element tree, so reconciliation can be tested without Minecraft. */
internal class TestNode(var name: String) {
    var parent: TestNode? = null
    val children = mutableListOf<TestNode>()

    /** Renders the subtree as `a(b,c)`. */
    override fun toString(): String =
        if (children.isEmpty()) name else "$name(${children.joinToString(",")})"
}

/** The same tree semantics as `UIElement`: adding a node takes it from its old parent. */
internal class TestTree : NodeTree<TestNode> {
    var inserts = 0
    var removes = 0
    var moves = 0

    override fun parentOf(node: TestNode) = node.parent
    override fun hostOf(node: TestNode) = node
    override fun childrenOf(host: TestNode): List<TestNode> = host.children

    override fun add(host: TestNode, child: TestNode, index: Int) {
        require(child.parent !== host) { "Cannot add the same child twice" }
        inserts++
        child.parent?.children?.remove(child)
        host.children.add(index, child)
        child.parent = host
    }

    override fun remove(host: TestNode, child: TestNode) {
        removes++
        host.children.remove(child)
        child.parent = null
    }

    override fun reorder(host: TestNode, order: List<TestNode>) {
        moves++
        host.children.clear()
        host.children.addAll(order)
    }
}

/** The real [TreeApplier] over [TestNode]s, counting what it does to the tree. */
internal open class TestApplier(root: TestNode, private val tree: TestTree = TestTree()) : TreeApplier<TestNode>(root, tree) {
    val inserts get() = tree.inserts
    val removes get() = tree.removes
    val moves get() = tree.moves

    fun resetCounts() {
        tree.inserts = 0
        tree.removes = 0
        tree.moves = 0
    }
}

@Composable
internal fun Node(name: String, content: @Composable () -> Unit = {}) {
    ComposeNode<TestNode, TestApplier>(
        factory = { TestNode(name) },
        update = { set(name) { this.name = it } },
        content = content,
    )
}

/** Emits an existing node, the way `Mount` emits an existing element. */
@Composable
internal fun Adopt(node: TestNode, content: @Composable () -> Unit = {}) {
    key(node) { ComposeNode<TestNode, TestApplier>(factory = { node }, update = {}, content = content) }
}

/** Queues continuations instead of running them, so [TestHarness.frame] decides when they run. */
private class QueueDispatcher : CoroutineDispatcher() {
    private val queue = ArrayDeque<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.addLast(block)
    }

    fun drain() {
        while (queue.isNotEmpty()) queue.removeFirst().run()
    }
}

/** Drives a composition one simulated frame at a time, in the same sequence as [ComposeHost.onFrame]. */
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

    /** One frame; [advanceMillis] is how far the frame clock moves, which animations see as elapsed time. */
    fun frame(advanceMillis: Long = 16) = frameOf(advanceMillis * 1_000_000)

    /** [frame] for periods that are not whole milliseconds. */
    fun frameOf(advanceNanos: Long) {
        Snapshot.sendApplyNotifications()
        dispatcher.drain()
        frameNanos += advanceNanos
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

    /** Mirrors [ComposeHost.dispose]; the final drain is what lets cancelled effects run their `finally`. */
    fun dispose() {
        composition.dispose()
        recomposer.cancel()
        scope.cancel()
        dispatcher.drain()
    }

    fun tree(): String = root.toString()

    /** The node named [name], anywhere in the tree. */
    fun find(name: String): TestNode {
        fun search(node: TestNode): TestNode? = if (node.name == name) node else node.children.firstNotNullOfOrNull(::search)
        return search(root) ?: error("no node named $name in ${tree()}")
    }

    /** The root's only child, for the common `root(container(...))` shape. */
    fun container(): TestNode = root.children.single()
}
