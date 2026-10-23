package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.AbstractApplier
import com.lowdragmc.lowdraglib2.LDLib2
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Translates a composition-relative child index into an index into the real child list, skipping
 * whatever the composition does not own.
 *
 * <p>An index one past the end maps to one past the *whole* list, so appending puts composed
 * children after everything — the same place [UIElement.addChild] would have put them.
 *
 * @param isOwned whether the child at a real index belongs to the composition. Taken as a predicate so
 *   the arithmetic can be tested without a live UI tree — `src/test` has no Minecraft on its classpath.
 */
internal inline fun realIndexOfComposed(count: Int, composedIndex: Int, isOwned: (Int) -> Boolean): Int {
    var seen = 0
    for (real in 0 until count) {
        if (!isOwned(real)) continue
        if (seen == composedIndex) return real
        seen++
    }
    return count
}

/**
 * Lays [order] over the entries of [children] that [moves] picks out, leaving every other entry in
 * the slot it already had.
 *
 * <p>How a reorder avoids disturbing children it does not own — a widget's internal parts or a child
 * added by hand for the applier, a header someone pinned outside the scroller for [TabView]. Pure, so
 * the arithmetic can be tested without a live tree.
 *
 * @return the full new child list, or null when [order] does not have one entry per picked-out child.
 *   Applying a permutation that disagrees with the tree would be worse than skipping it.
 */
internal inline fun <T> spliceOrder(children: List<T>, order: List<T>, moves: (T) -> Boolean): List<T>? {
    if (children.count(moves) != order.size) return null
    val spliced = ArrayList<T>(children.size)
    var next = 0
    for (child in children) spliced.add(if (moves(child)) order[next++] else child)
    return spliced
}

/**
 * Applies Compose's tree edits to a live [UIElement] tree.
 *
 * <p>Everything downstream of an edit is already automatic: attaching a child dirties the taffy
 * nodes all the way to the root, and registers the subtree with the host UI's style engine, which
 * re-runs selector matching against it. So this only has to do structure — never layout, never
 * styling.
 *
 * <p>It only ever touches the elements it inserted itself. A widget keeps its internal parts in the
 * same child list as everything else — `Button` builds its own label — and the element a composition
 * is hosted in can be given children by hand, as can the container of a [Subcompose]. Compose's
 * indices count only its own nodes, so every index is mapped over the owned children, and clearing a
 * host removes only those.
 *
 * <p>Every edit runs inside [CompositionWrites.writing]: a widget reacting to being attached, moved
 * or removed is not the user editing it.
 */
internal class UIApplier(root: UIElement) : AbstractApplier<UIElement>(root) {
    /** Every element this applier inserted and has not removed, at any depth. */
    private val owned: MutableSet<UIElement> = Collections.newSetFromMap(IdentityHashMap())

    private fun composedChildren(host: UIElement): List<UIElement> = host.children.filter { it in owned }

    private fun realIndex(host: UIElement, composedIndex: Int): Int {
        val children = host.children
        return realIndexOfComposed(children.size, composedIndex) { children[it] in owned }
    }

    /**
     * Left empty: subtrees are built detached and attached in one go, via [insertBottomUp]. That is
     * the cheaper half of the pair here, because attaching is what walks the subtree registering
     * every element with the host UI — doing it top-down would repeat that walk per node.
     */
    override fun insertTopDown(index: Int, instance: UIElement) = Unit

    override fun insertBottomUp(index: Int, instance: UIElement) = CompositionWrites.writing {
        val host = current.contentHost
        flushOrder(host)
        host.addChildAt(instance, realIndex(host, index))
        owned.add(instance)
        Unit
    }

    override fun remove(index: Int, count: Int) = CompositionWrites.writing {
        val host = current.contentHost
        flushOrder(host)
        val composed = composedChildren(host)
        // Back to front, so the earlier entries of the snapshot stay valid as we go.
        for (i in index + count - 1 downTo index) {
            val child = composed[i]
            host.removeChild(child)
            disown(child)
        }
    }

    /** Forgets a removed subtree, so the set does not keep discarded elements alive. */
    private fun disown(element: UIElement) {
        if (owned.remove(element)) {
            for (child in element.children) disown(child)
        }
    }

    /**
     * Composed children per host, while a batch of changes is being applied.
     *
     * <p>Compose emits a reorder as a run of individual moves. Sending each one through to the
     * element tree means re-deriving indices and touching the layout tree once per move, which is
     * quadratic over the run — a 500-row reversal took most of a second that way. Accumulating the
     * permutation in a plain list and handing the finished order over in [onEndChanges] costs one
     * pass, however many moves the run contains.
     */
    private val pendingOrder = HashMap<UIElement, MutableList<UIElement>>()

    override fun move(from: Int, to: Int, count: Int) {
        if (from == to || count <= 0) return
        val host = current.contentHost
        pendingOrder.getOrPut(host) { composedChildren(host).toMutableList() }.move(from, to, count)
    }

    override fun onEndChanges() {
        if (pendingOrder.isEmpty()) return
        CompositionWrites.writing {
            for (host in pendingOrder.keys.toList()) flushOrder(host)
        }
    }

    /**
     * Applies a host's accumulated permutation, if it has one.
     *
     * <p>Called before anything that would change which children exist, as well as at the end of the
     * batch: an insert or a removal reads and writes real child indices, so a permutation that is
     * still only recorded in memory has to land first or the two disagree.
     */
    private fun flushOrder(host: UIElement) {
        val composed = pendingOrder.remove(host) ?: return
        // Splice the permuted composed children back around the ones this applier does not own,
        // which never move.
        val reordered = spliceOrder(host.children, composed) { it in owned }
        if (reordered == null) {
            // Flushing before every structural change should make this impossible; if it does happen
            // the tree and the composition now disagree about the order, which is worth hearing about.
            LDLib2.LOGGER.warn("Compose skipped a reorder of {} that no longer matches its children", host)
            return
        }
        // Reordering rather than re-adding is what keeps a shuffled keyed list from losing focus
        // and restarting its animations.
        host.reorderChildren(reordered)
    }

    override fun onClear() = CompositionWrites.writing {
        pendingOrder.clear()
        val host = root.contentHost
        for (child in composedChildren(host)) {
            host.removeChild(child)
            disown(child)
        }
    }
}
