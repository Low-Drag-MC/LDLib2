package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator
import com.lowdragmc.lowdraglib2.configurator.IConfigurable
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView
import com.lowdragmc.lowdraglib2.gui.ui.elements.Inspector
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList
import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level

// The larger widgets: each builds a good deal of its own UI, so each wrapper is about which parts of
// that the composition should own and which it must leave alone.

/**
 * A tree of [ITreeNode]s with a controlled selection.
 *
 * <p>The widget builds and rebuilds its own rows, so there is no content slot: [nodeText] or
 * [nodeContent] says what a row shows. It also re-checks the expanded nodes' children every tick
 * (unless [staticTree]), so nodes added under a node already on screen appear by themselves; a
 * different [root] object is what makes it start over.
 *
 * <p>[onSelectedChange] reports the player's selections, as a copy. Rebuilding the tree clears the
 * widget's selection without that being reported, and [selected] is then put back.
 *
 * @param root the tree to show, compared by identity.
 * @param flattenRoot hide the root row and show its children as the top level.
 * @param multiSelect allow selecting several nodes.
 * @param nodeText a row's caption. Defaults to `Component.translatable(node.toString())`.
 * @param nodeContent a row's whole UI, for more than a caption. Wins over [nodeText].
 * @param onReorder drag-to-reorder: non-null turns it on and performs the move it is handed; the
 *   tree shows the result once the model has changed.
 * @param canReorder whether a hovered drop would be allowed. Null allows any.
 */
@Composable
fun <N : ITreeNode<*, *>> TreeList(
    root: N?,
    selected: Set<N>,
    onSelectedChange: (Set<N>) -> Unit,
    flattenRoot: Boolean = false,
    multiSelect: Boolean = false,
    staticTree: Boolean = false,
    nodeText: ((N) -> Component)? = null,
    nodeContent: ((N) -> UIElement)? = null,
    onDoubleClick: ((N) -> Unit)? = null,
    onReorder: ((TreeList.ReorderRequest<N>) -> Unit)? = null,
    canReorder: ((TreeList.ReorderRequest<N>) -> Boolean)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val callbacks = remember { TreeCallbacks<N>() }
    callbacks.onSelectedChange = onSelectedChange
    callbacks.nodeText = nodeText
    callbacks.nodeContent = nodeContent
    callbacks.onDoubleClick = onDoubleClick
    callbacks.onReorder = onReorder
    callbacks.canReorder = canReorder
    // Read so a refused selection recomposes this, and is then put back; see EditReporter.
    val edits = callbacks.edits
    UIElementNode(
        factory = {
            TreeList<N>().apply {
                setOnSelectedChanged { nodes ->
                    if (!CompositionWrites.active) {
                        callbacks.onSelectedChange?.invoke(nodes.toSet())
                        callbacks.edits++
                    }
                }
                setOnDoubleClickNode { node -> callbacks.onDoubleClick?.invoke(node) }
                setOnReorder { request -> callbacks.onReorder?.invoke(request) }
                setReorderValidator { request -> callbacks.canReorder?.invoke(request) ?: true }
                setNodeUISupplier { node ->
                    callbacks.nodeContent?.invoke(node)
                        ?: TreeList.textTemplate<N> { callbacks.nodeText?.invoke(it) ?: Component.translatable(it.toString()) }
                            .apply(node)
                }
            }
        },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        update = {
            set(edits) {}
            // Each of these rebuilds the rows, and that clears the selection with notification on;
            // all before the root, so a tree is only built once on the first frame.
            write(staticTree) { setStaticTree(it) }
            write(multiSelect) { setSupportMultipleSelection(it) }
            write(flattenRoot) { setFlattenRoot(it) }
            write(onReorder != null) { setDraggable(it) }
            write(Identity(root)) { setRoot(root) }
            reconcile {
                if (getSelected() != selected) CompositionWrites.writing { setSelected(selected, false) }
            }
        },
    )
}

private class TreeCallbacks<N> {
    var onSelectedChange: ((Set<N>) -> Unit)? = null
    var nodeText: ((N) -> Component)? = null
    var nodeContent: ((N) -> UIElement)? = null
    var onDoubleClick: ((N) -> Unit)? = null
    var onReorder: ((TreeList.ReorderRequest<N>) -> Unit)? = null
    var canReorder: ((TreeList.ReorderRequest<N>) -> Boolean)? = null
    var edits by mutableIntStateOf(0)
}

/**
 * A text box that searches as you type and offers the results in a drop-down.
 *
 * ```kotlin
 * SearchComponent(
 *     selected = item,
 *     onSelectedChange = { item = it },
 *     search = { query, found -> BuiltInRegistries.ITEM.forEach { if (matches(it, query)) found(it) } },
 *     resultText = { it.description.string },
 * )
 * ```
 *
 * <p>[search] runs on a background search thread, not the render thread: it may only read, and must
 * hand each result to `found` rather than touch the UI or composition state. A new keystroke abandons
 * the search before it.
 *
 * <p>Searching on the server — the widget's `setSearchOnServer` — registers an RPC, which a
 * composition must never do. Build such a component in `createUI` and [Mount] it.
 *
 * @param resultText what a result is shown as, in the box once picked. Defaults to `toString()`.
 * @param candidateContent a result's row in the drop-down. Defaults to [resultText] as text.
 */
@Composable
fun <T> SearchComponent(
    selected: T?,
    onSelectedChange: (T?) -> Unit,
    search: (query: String, found: (T) -> Unit) -> Unit,
    resultText: (T) -> String = { it.toString() },
    candidateContent: ((T) -> UIElement)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val current = remember { SearchCallbacks<T>() }
    current.search = search
    current.resultText = resultText
    current.candidateContent = candidateContent
    ControlledNode<SearchComponent<T?>, T?, T?>(
        factory = {
            // The search UI is installed once: replacing it disposes and recreates the widget's
            // search engine. It reads the latest lambdas instead.
            SearchComponent<T?>(object : SearchComponent.ISearchUI<T?> {
                override fun search(word: String, find: IResultHandler<T?>) {
                    current.search?.invoke(word) { find.acceptResult(it) }
                }

                override fun resultText(value: T?): String =
                    if (value == null) "" else current.resultText?.invoke(value) ?: value.toString()

                override fun onResultSelected(value: T?) = Unit
            }).apply {
                setCandidateUIProvider(UIElementProvider { candidate ->
                    val content = current.candidateContent
                    if (candidate != null && content != null) {
                        content(candidate)
                    } else {
                        UIElementProvider.text<T?> { Component.literal(if (it == null) "" else current.resultText?.invoke(it) ?: it.toString()) }
                            .apply(candidate)
                    }
                })
            }
        },
        value = selected,
        onValueChange = onSelectedChange,
        fromElement = { it },
        toElement = { it },
        layout, style, classes, id, enabled, visible, focusable, events,
    )
}

private class SearchCallbacks<T> {
    var search: ((String, (T) -> Unit) -> Unit)? = null
    var resultText: ((T) -> String)? = null
    var candidateContent: ((T) -> UIElement)? = null
}

/**
 * The editor's property inspector, showing [target]'s configurators.
 *
 * <p>Edits change [target] directly — that is what configurators do — and [onChange] reports each one.
 * A different target object is what makes it rebuild; the same object with new values is not, so a
 * target edited elsewhere should be inspected afresh by passing it again as a new object, or by
 * keying the call.
 *
 * @param historyStack where edits record undo entries. Null records none.
 */
@Composable
fun Inspector(
    target: IConfigurable?,
    onChange: ((Configurator) -> Unit)? = null,
    historyStack: IHistoryStack? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val currentOnChange = remember { Latest<((Configurator) -> Unit)?>(null) }
    currentOnChange.value = onChange
    UIElementNode(
        factory = { Inspector() },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        update = {
            // Before the target: inspecting records into whatever stack is set at the time.
            write(historyStack) { setHistoryStack(it) }
            write(Identity(target)) {
                if (target == null) clear() else inspect(target, { c -> currentOnChange.value?.invoke(c) })
            }
        },
    )
}

/**
 * A 3D view of blocks from a level.
 *
 * <p>The player can rotate, pan and zoom it ([draggable], [scalable]) and click a block face
 * ([onBlockSelected]); the camera is the widget's own, not composition state.
 *
 * @param world the level to render blocks from — usually the client's own. Null shows nothing.
 * @param blocks which positions to render. A different set re-renders them.
 * @param autoCamera frame the blocks whenever [blocks] changes.
 * @param orthographic an orthographic camera rather than a perspective one. Only read when the view
 *   is created.
 */
@Composable
fun Scene(
    world: Level?,
    blocks: Collection<BlockPos>,
    autoCamera: Boolean = true,
    onBlockSelected: ((BlockPos, Direction) -> Unit)? = null,
    draggable: Boolean = true,
    scalable: Boolean = true,
    interactive: Boolean = true,
    orthographic: Boolean = false,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
) {
    val currentOnSelected = remember { Latest<((BlockPos, Direction) -> Unit)?>(null) }
    currentOnSelected.value = onBlockSelected
    UIElementNode(
        factory = {
            Scene().apply {
                useOrtho(orthographic)
                setOnSelected { pos, face -> currentOnSelected.value?.invoke(pos, face) }
            }
        },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        update = {
            write(draggable) { setDraggable(it) }
            write(scalable) { setScalable(it) }
            write(interactive) { setIntractable(it) }
            // The scene before the blocks: rendering blocks needs a scene to render them in.
            write(Identity(world)) { if (world != null) createScene(world) }
            write(Identity(world) to blocks.toSet()) { (_, positions) ->
                if (world != null) setRenderedCore(positions, null, autoCamera)
            }
        },
    )
}

/**
 * A pannable, zoomable canvas. [content] is placed on it, positioned by its own `layout` (absolute
 * positions on the canvas); dragging the background pans and the wheel zooms.
 */
@Composable
fun GraphView(
    graphStyle: (GraphView.GraphViewStyle.() -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    val view = remember { GraphView() }
    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        widgetStyle = graphStyle?.let { block -> { getGraphViewStyle().apply(block) } },
    )
    // Into the panned and zoomed content root, not beside it: GraphView has no content host of its
    // own, so plain children would sit outside the transform.
    Subcompose(view.contentRoot, content)
}
