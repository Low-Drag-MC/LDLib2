package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.AbstractApplier
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.IdentityHashMap
import java.util.WeakHashMap

/** The tree operations [TreeApplier] needs, so the same logic drives `UIElement`s and test nodes. */
internal interface NodeTree<N : Any> {
    fun parentOf(node: N): N?

    /** The node a node's composed children are added to: itself, or a widget's inner container. */
    fun hostOf(node: N): N

    fun childrenOf(host: N): List<N>

    /** Adds [child] to [host] at [index], taking it from any parent it has. */
    fun add(host: N, child: N, index: Int)

    fun remove(host: N, child: N)

    /** Puts [host]'s children in [order], a permutation of them. */
    fun reorder(host: N, order: List<N>)
}

/**
 * Where a composed child goes in its host's real child list: before the next composed sibling still in
 * that list, else after the previous one, else at the end. That keeps the composed children together
 * and in composition order, wherever the children the composition does not own sit.
 *
 * @param siblings the composed children in composition order, the new one already at [index].
 * @param count the size of the real child list.
 * @param positionOf a sibling's index in the real child list, or -1 when it is not there.
 */
internal inline fun <T> insertionIndex(siblings: List<T>, index: Int, count: Int, positionOf: (T) -> Int): Int {
    for (i in index + 1 until siblings.size) {
        val at = positionOf(siblings[i])
        if (at >= 0) return at
    }
    for (i in index - 1 downTo 0) {
        val at = positionOf(siblings[i])
        if (at >= 0) return at + 1
    }
    return count
}

/**
 * Lays [order] over the entries of [children] that [moves] picks out, leaving every other entry in
 * the slot it already had.
 *
 * @return the full new child list, or null when [order] does not have one entry per picked-out child.
 */
internal inline fun <T> spliceOrder(children: List<T>, order: List<T>, moves: (T) -> Boolean): List<T>? {
    if (children.count(moves) != order.size) return null
    val spliced = ArrayList<T>(children.size)
    var next = 0
    for (child in children) spliced.add(if (moves(child)) order[next++] else child)
    return spliced
}

/**
 * Applies Compose's tree edits to a live tree that also holds children the composition did not create
 * (a widget's internal parts, children added by hand), and whose nodes can be taken by another parent.
 *
 * <p>Indices come from Compose's own record of each node's children, never from the real child lists;
 * the real tree is only touched where it still agrees with that record.
 */
internal open class TreeApplier<N : Any>(root: N, private val tree: NodeTree<N>) : AbstractApplier<N>(root) {
    /** Each node's children as the composition sees them. */
    private val composed = IdentityHashMap<N, MutableList<N>>()

    /** Removed in this batch, and forgotten at its end unless something put them back. */
    private val removed: MutableSet<N> = Collections.newSetFromMap(IdentityHashMap())

    /** Nodes whose children moved in this batch, reordered once at its end. */
    private val reordered: MutableSet<N> = Collections.newSetFromMap(IdentityHashMap())

    /**
     * The composed children of nodes that left the tree, weak both ways. Movable content puts a node
     * back with them in a later batch; a `remember`ed node re-emitted by a new node must lose them,
     * since Compose never removes a removed node's descendants.
     */
    private val leftovers = WeakHashMap<N, List<WeakReference<N>>>()

    /** Movable content is put back with insertBottomUp and then insertTopDown; a new node the other way round. */
    private var lastInsertedBottomUp: N? = null

    /** Wraps every change to the tree. */
    protected open fun <R> edit(block: () -> R): R = block()

    /** Called when a node's recorded order no longer matches its children, and the reorder is skipped. */
    protected open fun reorderSkipped(host: N) = Unit

    private fun childrenOf(node: N) = composed.getOrPut(node) { ArrayList() }

    override fun insertTopDown(index: Int, instance: N) {
        val putBack = instance === lastInsertedBottomUp
        lastInsertedBottomUp = null
        if (putBack) {
            restore(instance)
            return
        }
        if (removed.remove(instance)) retire(instance)
        val stale = leftovers.remove(instance) ?: return
        edit {
            val host = tree.hostOf(instance)
            for (ref in stale) ref.get()?.takeIf { tree.parentOf(it) === host }?.let { tree.remove(host, it) }
        }
    }

    override fun insertBottomUp(index: Int, instance: N) = edit {
        removed.remove(instance)
        val siblings = childrenOf(current)
        siblings.add(index, instance)
        val host = tree.hostOf(current)
        val live = tree.childrenOf(host)
        val at = insertionIndex(siblings, index, live.size) { if (tree.parentOf(it) === host) live.indexOf(it) else -1 }
        tree.add(host, instance, at)
        lastInsertedBottomUp = instance
    }

    override fun remove(index: Int, count: Int) = edit {
        lastInsertedBottomUp = null
        val siblings = childrenOf(current)
        val host = tree.hostOf(current)
        repeat(count) {
            val child = siblings.removeAt(index)
            when (tree.parentOf(child)) {
                host -> {
                    tree.remove(host, child)
                    removed.add(child)
                }
                null -> removed.add(child)
                // Taken by another parent's insert earlier in this batch: it lives on there.
                else -> Unit
            }
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        lastInsertedBottomUp = null
        if (from == to || count <= 0) return
        childrenOf(current).move(from, to, count)
        reordered.add(current)
    }

    override fun onEndChanges() = edit {
        lastInsertedBottomUp = null
        for (node in reordered) applyOrder(node)
        reordered.clear()
        for (node in removed) retire(node)
        removed.clear()
    }

    /**
     * Puts a node's composed children in their recorded order with one reorder, however many moves
     * the batch made: applying each move to the tree is quadratic over a large shuffle.
     */
    private fun applyOrder(node: N) {
        val host = tree.hostOf(node)
        val order = composed[node]?.filter { tree.parentOf(it) === host } ?: return
        val members: MutableSet<N> = Collections.newSetFromMap(IdentityHashMap())
        members.addAll(order)
        val spliced = spliceOrder(tree.childrenOf(host), order) { it in members }
        if (spliced == null) reorderSkipped(host) else tree.reorder(host, spliced)
    }

    /** Moves a node's subtree from [composed] to [leftovers]. */
    private fun retire(node: N) {
        val children = composed.remove(node) ?: return
        leftovers[node] = children.map(::WeakReference)
        for (child in children) retire(child)
    }

    /** The other way, for movable content put back. */
    private fun restore(node: N) {
        val kept = leftovers.remove(node) ?: return
        val children = kept.mapNotNullTo(ArrayList()) { it.get() }
        composed[node] = children
        for (child in children) restore(child)
    }

    override fun onClear() = edit {
        reordered.clear()
        val host = tree.hostOf(root)
        for (child in childrenOf(root)) {
            if (tree.parentOf(child) === host) tree.remove(host, child)
            retire(child)
        }
        composed.clear()
        removed.clear()
    }
}
