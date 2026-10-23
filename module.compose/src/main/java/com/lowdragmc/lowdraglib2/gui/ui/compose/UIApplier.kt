package com.lowdragmc.lowdraglib2.gui.ui.compose

import com.lowdragmc.lowdraglib2.LDLib2
import com.lowdragmc.lowdraglib2.gui.ui.UIElement

private object UIElementTree : NodeTree<UIElement> {
    override fun parentOf(node: UIElement): UIElement? = node.parent
    override fun hostOf(node: UIElement): UIElement = node.contentHost
    override fun childrenOf(host: UIElement): List<UIElement> = host.children
    override fun add(host: UIElement, child: UIElement, index: Int) {
        host.addChildAt(child, index)
    }
    override fun remove(host: UIElement, child: UIElement) {
        host.removeChild(child)
    }
    override fun reorder(host: UIElement, order: List<UIElement>) = host.reorderChildren(order)
}

/**
 * Applies Compose's tree edits to LDLib2's element tree, each inside [CompositionWrites.writing]: a
 * widget reacting to being attached, moved or removed is not the user editing it.
 */
internal class UIApplier(root: UIElement) : TreeApplier<UIElement>(root, UIElementTree) {
    override fun <R> edit(block: () -> R): R = CompositionWrites.writing(block)

    override fun reorderSkipped(host: UIElement) {
        LDLib2.LOGGER.warn("Compose skipped a reorder of {} that no longer matches its children", host)
    }
}
