package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.AbstractApplier
import com.lowdragmc.lowdraglib2.gui.ui.UIElement

/**
 * The children a composition owns, in document order.
 *
 * <p>Deliberately not all children: a widget assembled out of internal parts keeps those in the same
 * list, and they are none of the composition's business. [com.lowdragmc.lowdraglib2.gui.ui.elements.Button]
 * builds its own label in its constructor, so without this filter the first composed child of a
 * `Button { }` would be inserted at index 0 — in front of the button's own text.
 */
internal val UIElement.composedChildren: List<UIElement>
    get() = children.filter { !it.isInternalUI }

/**
 * Translates a composition-relative child index into an index into the real child list, skipping
 * whatever internal parts the element keeps in there.
 *
 * <p>An index one past the end maps to one past the *whole* list, so appending puts composed
 * children after everything — the same place [UIElement.addChild] would have put them.
 */
internal fun UIElement.realIndexOfComposed(composedIndex: Int): Int {
    val kids = children
    return realIndexOfComposed(kids.size, composedIndex) { kids[it].isInternalUI }
}

/**
 * The same arithmetic over a list of internal-or-not flags.
 *
 * <p>Exists so the mapping can be tested without a live UI tree — `src/test` has no Minecraft on its
 * classpath, and this is the part that is easy to get subtly wrong.
 */
internal fun realIndexOfComposed(internalFlags: List<Boolean>, composedIndex: Int): Int =
    realIndexOfComposed(internalFlags.size, composedIndex) { internalFlags[it] }

/**
 * The one implementation both of the above share.
 *
 * <p>Inline and predicate-driven rather than taking a list: the element overload runs once per
 * insert, so materialising a list of boxed flags there would cost an allocation per element on the
 * path that builds a tree.
 */
private inline fun realIndexOfComposed(count: Int, composedIndex: Int, isInternal: (Int) -> Boolean): Int {
    var seen = 0
    for (real in 0 until count) {
        if (isInternal(real)) continue
        if (seen == composedIndex) return real
        seen++
    }
    return count
}

/**
 * Applies Compose's tree edits to a live [UIElement] tree.
 *
 * <p>Everything downstream of an edit is already automatic: attaching a child dirties the taffy
 * nodes all the way to the root, and registers the subtree with the host UI's style engine, which
 * re-runs selector matching against it. So this only has to do structure — never layout, never
 * styling.
 */
internal class UIApplier(root: UIElement) : AbstractApplier<UIElement>(root) {

    /**
     * Left empty: subtrees are built detached and attached in one go, via [insertBottomUp]. That is
     * the cheaper half of the pair here, because attaching is what walks the subtree registering
     * every element with the host UI — doing it top-down would repeat that walk per node.
     */
    override fun insertTopDown(index: Int, instance: UIElement) = Unit

    override fun insertBottomUp(index: Int, instance: UIElement) {
        val host = current.contentHost
        flushOrder(host)
        host.addChildAt(instance, host.realIndexOfComposed(index))
    }

    override fun remove(index: Int, count: Int) {
        val host = current.contentHost
        flushOrder(host)
        val composed = host.composedChildren
        // Back to front, so the earlier entries of the snapshot stay valid as we go.
        for (i in index + count - 1 downTo index) {
            host.removeChild(composed[i])
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
        pendingOrder.getOrPut(host) { host.composedChildren.toMutableList() }.move(from, to, count)
    }

    override fun onEndChanges() {
        if (pendingOrder.isEmpty()) return
        for (host in pendingOrder.keys.toList()) flushOrder(host)
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
        val children = host.children
        // Defensive: flushing before every structural change should keep these in step. If they are
        // not, applying a stale permutation would be worse than skipping it.
        if (composed.size != children.count { !it.isInternalUI }) return

        // Splice the permuted composed children back around the internal ones, which never move.
        val reordered = ArrayList<UIElement>(children.size)
        var next = 0
        for (child in children) reordered.add(if (child.isInternalUI) child else composed[next++])
        // Reordering rather than re-adding is what keeps a shuffled keyed list from losing focus
        // and restarting its animations.
        host.reorderChildren(reordered)
    }

    override fun onClear() {
        pendingOrder.clear()
        val host = root.contentHost
        for (child in host.composedChildren) {
            host.removeChild(child)
        }
    }
}
