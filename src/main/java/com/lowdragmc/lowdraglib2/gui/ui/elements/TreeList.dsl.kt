package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Extension function for TreeList.TreeListStyle DSL
 */
fun <NODE : ITreeNode<*, *>> TreeList<NODE>.treeListStyleDsl(init: TreeList<NODE>.TreeListStyle.() -> Unit = {}): TreeList<NODE> {
    this.menuStyle(init)
    return this
}

/**
 * Specification for TreeList element
 */
open class TreeListSpec<NODE : ITreeNode<*, *>, T : TreeList<NODE>>(
    var treeListStyle: (TreeList<NODE>.TreeListStyle.() -> Unit)? = null,
    var root: NODE? = null,
    var staticTree: Boolean? = null,
    var flattenRoot: Boolean? = null,
    var supportMultipleSelection: Boolean? = null,
    var clickToExpand: Boolean? = null,
    var doubleClickToExpand: Boolean? = null,
    var nodeUISupplier: UIElementProvider<NODE>? = null,
    var onNodeUICreated: BiConsumer<NODE, UIElement>? = null,
    var selectedNodes: Collection<NODE>? = null,
    var onSelectedChanged: Consumer<Set<NODE>>? = null,
    var onDoubleClickNode: Consumer<NODE>? = null,
    var selectableNodeFilter: Predicate<NODE>? = null,
    var expandedNodes: Collection<NODE>? = null,
) : ElementSpec<T>() {
    /**
     * Set root node
     */
    fun root(node: NODE) = apply {
        this.root = node
    }

    /**
     * Enable static tree mode (no dynamic updates)
     */
    fun staticTree() = apply {
        this.staticTree = true
    }

    /**
     * Flatten root (show only root's children)
     */
    fun flattenRoot() = apply {
        this.flattenRoot = true
    }

    /**
     * Enable multi-selection with Ctrl/Shift
     */
    fun multiSelect() = apply {
        this.supportMultipleSelection = true
    }

    /**
     * Expand/collapse on single click
     */
    fun clickToExpand() = apply {
        this.clickToExpand = true
    }

    /**
     * Expand/collapse on double click
     */
    fun doubleClickToExpand() = apply {
        this.doubleClickToExpand = true
    }

    /**
     * Set node UI provider
     */
    fun nodeUI(supplier: UIElementProvider<NODE>) = apply {
        this.nodeUISupplier = supplier
    }

    /**
     * Set node UI provider (Kotlin lambda)
     */
    fun nodeUI(supplier: (NODE) -> UIElement) = apply {
        this.nodeUISupplier = UIElementProvider { supplier(it) }
    }

    /**
     * Set node UI created callback
     */
    fun onNodeUICreated(handler: BiConsumer<NODE, UIElement>) = apply {
        this.onNodeUICreated = handler
    }

    /**
     * Set node UI created callback (Kotlin lambda)
     */
    fun onNodeUICreated(handler: (NODE, UIElement) -> Unit) = apply {
        this.onNodeUICreated = BiConsumer { node, ui -> handler(node, ui) }
    }

    /**
     * Set selection changed listener
     */
    fun onSelectionChanged(handler: Consumer<Set<NODE>>) = apply {
        this.onSelectedChanged = handler
    }

    /**
     * Set selection changed listener (Kotlin lambda)
     */
    fun onSelectionChanged(handler: (Set<NODE>) -> Unit) = apply {
        this.onSelectedChanged = Consumer { handler(it) }
    }

    /**
     * Set double-click listener
     */
    fun onDoubleClick(handler: Consumer<NODE>) = apply {
        this.onDoubleClickNode = handler
    }

    /**
     * Set double-click listener (Kotlin lambda)
     */
    fun onDoubleClick(handler: (NODE) -> Unit) = apply {
        this.onDoubleClickNode = Consumer { handler(it) }
    }

    /**
     * Set selectable node filter
     */
    fun selectableFilter(filter: Predicate<NODE>) = apply {
        this.selectableNodeFilter = filter
    }

    /**
     * Set selectable node filter (Kotlin lambda)
     */
    fun selectableFilter(filter: (NODE) -> Boolean) = apply {
        this.selectableNodeFilter = Predicate { filter(it) }
    }

    /**
     * Set initially selected nodes
     */
    fun selected(vararg nodes: NODE) = apply {
        this.selectedNodes = nodes.toList()
    }

    /**
     * Set initially expanded nodes
     */
    fun expanded(vararg nodes: NODE) = apply {
        this.expandedNodes = nodes.toList()
    }
}

/**
 * TreeList element builder
 */
open class TreeListElement<NODE : ITreeNode<*, *>, T : TreeList<NODE>>(
    element: T,
    spec: (TreeListSpec<NODE, T>.() -> Unit)? = null,
) : UIContainer<T, TreeListSpec<NODE, T>>(element, spec) {
    override fun makeSpec(): TreeListSpec<NODE, T>? {
        return spec?.let { TreeListSpec<NODE, T>().apply(it) }
    }

    override fun build(spec: TreeListSpec<NODE, T>?): T {
        val e = super.build(spec)
        applyTreeListProperties(spec, e)
        return e
    }

    protected fun applyTreeListProperties(spec: TreeListSpec<NODE, T>?, element: TreeList<NODE>) {
        spec?.treeListStyle?.let { element.menuStyle(it) }
        spec?.staticTree?.let { element.staticTree = it }
        spec?.flattenRoot?.let { element.flattenRoot = it }
        spec?.supportMultipleSelection?.let { element.supportMultipleSelection = it }
        spec?.clickToExpand?.let { element.clickToExpand = it }
        spec?.doubleClickToExpand?.let { element.doubleClickToExpand = it }
        spec?.nodeUISupplier?.let { element.setNodeUISupplier(it) }
        spec?.onNodeUICreated?.let { element.setOnNodeUICreated(it) }
        spec?.selectableNodeFilter?.let { element.selectableNodeFilter = it }
        spec?.onDoubleClickNode?.let { element.onDoubleClickNode = it }

        // Apply root before selection/expansion
        spec?.root?.let { element.setRoot(it) }

        // Apply expansion before selection
        spec?.expandedNodes?.let { nodes ->
            nodes.forEach { element.expandNode(it) }
        }

        // Apply selection
        spec?.selectedNodes?.let { element.setSelected(it, false) }

        // Set selection listener after initial selection
        spec?.onSelectedChanged?.let { element.onSelectedChanged = it }
    }
}

/**
 * Top Level - Create a standalone TreeList element
 */
fun <NODE : ITreeNode<*, *>> treeList(spec: (TreeListSpec<NODE, TreeList<NODE>>.() -> Unit)? = null,
                                       init: TreeListElement<NODE, TreeList<NODE>>.() -> Unit = {}): TreeList<NODE> {
    return TreeListElement(TreeList<NODE>(), spec).apply(init).build()
}

/**
 * Top Level - Create TreeList with root node
 */
fun <NODE : ITreeNode<*, *>> treeList(root: NODE,
                                       spec: (TreeListSpec<NODE, TreeList<NODE>>.() -> Unit)? = null,
                                       init: TreeListElement<NODE, TreeList<NODE>>.() -> Unit = {}): TreeList<NODE> {
    return TreeListElement(TreeList<NODE>(root), spec).apply(init).build()
}

/**
 * Top Level - Create TreeList with root node and static mode
 */
fun <NODE : ITreeNode<*, *>> treeList(root: NODE,
                                       staticTree: Boolean,
                                       spec: (TreeListSpec<NODE, TreeList<NODE>>.() -> Unit)? = null,
                                       init: TreeListElement<NODE, TreeList<NODE>>.() -> Unit = {}): TreeList<NODE> {
    return TreeListElement(TreeList<NODE>(root, staticTree), spec).apply(init).build()
}

/**
 * Internal Builder - Add TreeList as a child to a container
 */
fun <NODE : ITreeNode<*, *>> UIContainer<*, *>.treeList(spec: (TreeListSpec<NODE, TreeList<NODE>>.() -> Unit)? = null,
                                                          init: TreeListElement<NODE, TreeList<NODE>>.() -> Unit = {}) =
    add(TreeListElement(TreeList<NODE>(), spec), init)

/**
 * Internal Builder - Add TreeList with root as a child to a container
 */
fun <NODE : ITreeNode<*, *>> UIContainer<*, *>.treeList(root: NODE,
                                                          spec: (TreeListSpec<NODE, TreeList<NODE>>.() -> Unit)? = null,
                                                          init: TreeListElement<NODE, TreeList<NODE>>.() -> Unit = {}) =
    add(TreeListElement(TreeList<NODE>(root), spec), init)

/**
 * DSL converter - Convert existing TreeList to DSL builder
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> T.dsl(spec: (TreeListSpec<NODE, T>.() -> Unit)? = null,
                                                         init: TreeListElement<NODE, T>.() -> Unit = {}): TreeListElement<NODE, T> {
    return TreeListElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set root node
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.withRoot(root: NODE): TreeListElement<NODE, T> = apply {
    element.setRoot(root)
}

/**
 * Extension: Flatten root
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.flattenRoot(flatten: Boolean = true): TreeListElement<NODE, T> = apply {
    element.setFlattenRoot(flatten)
}

/**
 * Extension: Set node UI supplier
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.withNodeUI(supplier: (NODE) -> UIElement): TreeListElement<NODE, T> = apply {
    element.setNodeUISupplier(UIElementProvider { supplier(it) })
}

/**
 * Extension: Set node UI created callback
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.onNodeUICreated(handler: (NODE, UIElement) -> Unit): TreeListElement<NODE, T> = apply {
    element.setOnNodeUICreated(BiConsumer { node, ui -> handler(node, ui) })
}

/**
 * Extension: Enable multi-selection
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.multiSelect(enable: Boolean = true): TreeListElement<NODE, T> = apply {
    element.supportMultipleSelection = enable
}

/**
 * Extension: Set selected nodes
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.withSelected(vararg nodes: NODE): TreeListElement<NODE, T> = apply {
    element.setSelected(nodes.toSet(), false)
}

/**
 * Extension: Set selection changed listener
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.onSelectionChange(handler: (Set<NODE>) -> Unit): TreeListElement<NODE, T> = apply {
    element.onSelectedChanged = Consumer { handler(it) }
}

/**
 * Extension: Set double-click listener
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.onDoubleClick(handler: (NODE) -> Unit): TreeListElement<NODE, T> = apply {
    element.onDoubleClickNode = Consumer { handler(it) }
}

/**
 * Extension: Expand specific nodes
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.expand(vararg nodes: NODE): TreeListElement<NODE, T> = apply {
    nodes.forEach { element.expandNode(it) }
}

/**
 * Extension: Collapse specific nodes
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.collapse(vararg nodes: NODE): TreeListElement<NODE, T> = apply {
    nodes.forEach { element.collapseNode(it) }
}

/**
 * Extension: Expand node and all parents
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.expandPath(node: NODE): TreeListElement<NODE, T> = apply {
    element.expandNodeAlongPath(node)
}

/**
 * Extension: Reload tree
 */
fun <NODE : ITreeNode<*, *>, T : TreeList<NODE>> TreeListElement<NODE, T>.reload(): TreeListElement<NODE, T> = apply {
    element.reloadList()
}
