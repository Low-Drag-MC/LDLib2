package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.getValue
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import net.minecraft.network.chat.Component
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab as LdlTab
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView as LdlTabView

/** Collects the tabs declared inside a [TabView] block. */
@Stable
class TabScope internal constructor() {
    internal val specs = mutableListOf<TabSpec>()

    /**
     * Declares one tab.
     *
     * @param key identifies the tab across recompositions. A tab keeps its element, its selection
     *   and everything its content remembered for as long as the same key keeps appearing — so use
     *   something stable, not the index. Its `rememberSaveable` state outlives even that: a tab
     *   removed and later declared again under the same key gets it back.
     */
    fun tab(key: Any, title: Component, content: @Composable () -> Unit) {
        specs += TabSpec(key, title, content)
    }

    fun tab(key: Any, title: String, content: @Composable () -> Unit) =
        tab(key, Component.literal(title), content)
}

internal class TabSpec(
    val key: Any,
    val title: Component,
    val content: @Composable () -> Unit,
)

/**
 * A tabbed container.
 *
 * ```kotlin
 * TabView(selectedKey = current, onSelect = { current = it }) {
 *     tab("general", "General") { GeneralPage() }
 *     tab("advanced", "Advanced") { AdvancedPage() }
 * }
 * ```
 *
 * <p>Each tab's body is a [Subcompose] into that tab's content pane.
 *
 * <p>The headers follow the declared order, also when an existing set is reordered; a reorder keeps
 * the selection and everything each tab's content remembered. Tab keys must be unique: a repeated key
 * throws [IllegalArgumentException].
 *
 * @param selectedKey which tab to show. Leave null to let the widget manage its own selection. A
 *   header click still switches tabs on screen whatever this says, so hoisted selection should update
 *   from [onSelect].
 * @param onSelect notified with the tab's key when the player picks a tab, and once when the selected
 *   tab is removed and [selectedKey] does not name a tab that is still there: the selection then moves
 *   to the removed tab's next surviving neighbour in the old declared order, or the previous one, or
 *   failing both the first tab now declared. Not for selections this composable makes itself —
 *   following [selectedKey], picking the first tab to begin with — and not when the whole TabView
 *   leaves the composition.
 */
@Composable
fun TabView(
    selectedKey: Any? = null,
    onSelect: ((Any) -> Unit)? = null,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    content: TabScope.() -> Unit,
) {
    val specs = TabScope().apply(content).specs
    val order = specs.map { it.key }
    require(order.toSet().size == order.size) {
        "TabView tab keys must be unique; repeated: ${order.groupBy { it }.filterValues { it.size > 1 }.keys}"
    }
    val view = remember { LdlTabView() }
    val keysByTab = remember { HashMap<LdlTab, Any>() }
    val tabsByKey = remember { HashMap<Any, LdlTab>() }
    val currentOnSelect by rememberUpdatedState(onSelect)
    // Keeps each tab's saved state apart, and a removed tab's until it comes back.
    val tabStates = rememberSaveableStateHolder()
    val lastOrder = remember { arrayOf<List<Any>>(emptyList()) }
    val leaving = remember { BooleanArray(1) }
    val removedSelection = remember { RemovedSelection() }

    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible,
    )

    DisposableEffect(view) {
        // The widget also reports the selections it makes itself on add and remove; only the player's count.
        view.setOnTabSelected { tab ->
            if (!CompositionWrites.active) keysByTab[tab]?.let { currentOnSelect?.invoke(it) }
        }
        onDispose { view.setOnTabSelected { } }
    }

    specs.forEach { spec ->
        key(spec.key) {
            val tab = remember { LdlTab() }
            // The widget shows and hides this pane on selection; the content inside keeps its own display flag.
            val host = remember { UIElement().addClass("__compose_tab_content__") }

            DisposableEffect(view) {
                keysByTab[tab] = spec.key
                tabsByKey[spec.key] = tab
                CompositionWrites.writing { view.addTab(tab, host) }
                onDispose {
                    val wasSelected = view.selectedTab === tab
                    // removeTab's own fallback is arbitrary (hash order); the selection effect below
                    // makes the real choice once every removal and the new selectedKey are in.
                    CompositionWrites.writing { view.removeTab(tab) }
                    keysByTab.remove(tab)
                    tabsByKey.remove(spec.key)
                    // Only the first: that fallback may have selected a tab that is leaving as well.
                    if (wasSelected && !leaving[0] && removedSelection.key == null) {
                        removedSelection.key = spec.key
                        removedSelection.order = lastOrder[0]
                    }
                }
            }
            // Keyed on the title: setText re-measures the header.
            DisposableEffect(spec.title) {
                tab.setText(spec.title)
                onDispose { }
            }
            Subcompose(host) { tabStates.SaveableStateProvider(spec.key, spec.content) }
        }
    }

    // After the tabs: effects dispose in reverse order, so `leaving` is set before the tabs remove themselves.
    DisposableEffect(Unit) {
        onDispose { leaving[0] = true }
    }

    // After the tabs exist. `addTab` appends and a moved tab never passes through it again, so the
    // declared order is imposed here.
    DisposableEffect(order) {
        lastOrder[0] = order
        val headers = order.mapNotNull { tabsByKey[it] }
        val owned = headers.toHashSet()
        val container = view.tabScroller.viewContainer
        // Headers we did not add keep their slots; null means one of ours left the scroller, so skip.
        spliceOrder(container.children, headers) { it in owned }?.let { container.reorderChildren(it) }
        onDispose { }
    }

    // After the tabs exist: selecting a key not yet added does nothing. Keyed on the order too, so a
    // just-arrived selected tab is applied and every removed selection is resolved here, once.
    DisposableEffect(selectedKey, order) {
        val removed = removedSelection.key
        val removedFrom = removedSelection.order
        removedSelection.key = null
        removedSelection.order = emptyList()
        val wanted = selectedKey?.let { tabsByKey[it] }
        if (wanted != null) {
            if (view.selectedTab !== wanted) CompositionWrites.writing { view.selectTab(wanted) }
        } else if (removed != null) {
            neighbourOf(removed, removedFrom, order)?.let { next ->
                tabsByKey[next]?.let { tab -> CompositionWrites.writing { view.selectTab(tab) } }
                currentOnSelect?.invoke(next)
            }
        }
        onDispose { }
    }
}

private class RemovedSelection {
    var key: Any? = null
    var order: List<Any> = emptyList()
}

/**
 * The tab to select when [removed], the selected one, goes: the next one after it in [oldOrder] that
 * is still in [newOrder], or failing that the previous one, or failing both the first of [newOrder].
 * Null when no tab is left.
 */
internal fun neighbourOf(removed: Any, oldOrder: List<Any>, newOrder: List<Any>): Any? {
    val remaining = newOrder.toHashSet()
    val at = oldOrder.indexOf(removed)
    if (at >= 0) {
        for (i in at + 1 until oldOrder.size) if (oldOrder[i] in remaining) return oldOrder[i]
        for (i in at - 1 downTo 0) if (oldOrder[i] in remaining) return oldOrder[i]
    }
    return newOrder.firstOrNull()
}
