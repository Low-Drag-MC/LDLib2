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
 * <p>Each tab's body is its own [Subcompose], pointed at the content element the widget hands out —
 * the same arrangement the split view uses, for the same reason: a tab view has one child list per
 * tab, and an applier only knows one.
 *
 * <p>The headers follow the order the tabs are declared in, including when an existing set is
 * reordered. A reorder moves the headers in place rather than removing and re-adding them, so the
 * selection, and everything each tab's content remembered, stays put. Tab keys must be unique.
 *
 * @param selectedKey which tab to show. Leave null to let the widget manage its own selection. A
 *   header click still switches tabs on screen whatever this says, so hoisted selection should update
 *   from [onSelect].
 * @param onSelect notified with the tab's key when the player picks a tab, and when the selected tab is
 *   removed and the selection moves to its neighbour in declared order. Not for selections this
 *   composable makes itself — following [selectedKey], picking the first tab to begin with — and not
 *   when the whole TabView leaves the composition.
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
    // Tab element -> key, so a selection coming back out of the widget can be reported as the key
    // the caller declared rather than as an element they have never seen.
    val keysByTab = remember { HashMap<LdlTab, Any>() }
    val tabsByKey = remember { HashMap<Any, LdlTab>() }
    val currentOnSelect by rememberUpdatedState(onSelect)
    // Where a removed tab's rememberSaveable state waits, in case the tab comes back. Also what keeps
    // the tabs' saved state apart: each one saves under its own key.
    val tabStates = rememberSaveableStateHolder()
    // The declared order last applied, for picking a removed tab's neighbour; and whether the whole
    // view is on its way out, in which case nobody is told about a selection.
    val lastOrder = remember { arrayOf<List<Any>>(emptyList()) }
    val leaving = remember { BooleanArray(1) }

    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible,
    )

    DisposableEffect(view) {
        // The widget reports every selection, including the ones it makes itself when a tab is added
        // or removed. Only the player's count.
        view.setOnTabSelected { tab ->
            if (!CompositionWrites.active) keysByTab[tab]?.let { currentOnSelect?.invoke(it) }
        }
        onDispose { view.setOnTabSelected { } }
    }

    specs.forEach { spec ->
        key(spec.key) {
            val tab = remember { LdlTab() }
            // Classed so stylesheets can reach the pane. It is also the element the widget shows and
            // hides on selection — the content inside it keeps its own display flag either way.
            val host = remember { UIElement().addClass("__compose_tab_content__") }

            DisposableEffect(view) {
                keysByTab[tab] = spec.key
                tabsByKey[spec.key] = tab
                CompositionWrites.writing { view.addTab(tab, host) }
                onDispose {
                    val wasSelected = view.selectedTab === tab
                    // removeTab falls back to whichever tab its hash map yields first; the neighbour in
                    // declared order is picked below instead.
                    CompositionWrites.writing { view.removeTab(tab) }
                    keysByTab.remove(tab)
                    tabsByKey.remove(spec.key)
                    if (wasSelected && !leaving[0]) {
                        neighbourOf(spec.key, lastOrder[0], tabsByKey)?.let { next ->
                            CompositionWrites.writing { view.selectTab(tabsByKey.getValue(next)) }
                            currentOnSelect?.invoke(next)
                        }
                    }
                }
            }
            // Keyed on the title so an unchanged caption is not re-set every recomposition; setting
            // text re-measures the header.
            DisposableEffect(spec.title) {
                tab.setText(spec.title)
                onDispose { }
            }
            Subcompose(host) { tabStates.SaveableStateProvider(spec.key, spec.content) }
        }
    }

    // Declared after the tabs, so it is forgotten before them when the whole view leaves: effects are
    // disposed in reverse order. By the time the tabs remove themselves, this has said not to report.
    DisposableEffect(Unit) {
        onDispose { leaving[0] = true }
    }

    // After the tabs exist, for the same reason as the selection below. `addTab` appends, and a tab
    // that only moved never passes through it again, so the declared order is imposed from here.
    DisposableEffect(order) {
        lastOrder[0] = order
        val headers = order.mapNotNull { tabsByKey[it] }
        val owned = headers.toHashSet()
        val container = view.tabScroller.viewContainer
        // Headers this composable did not put there keep their slots. Null means one of ours has
        // been moved out of the scroller, which leaves nothing sensible to reorder.
        spliceOrder(container.children, headers) { it in owned }?.let { container.reorderChildren(it) }
        onDispose { }
    }

    // After the tabs exist, not before: selecting a key that has not been added yet does nothing.
    // Keyed on the order too, so a selected key whose tab only just arrived is applied.
    DisposableEffect(selectedKey, order) {
        if (selectedKey != null) {
            tabsByKey[selectedKey]?.let { tab ->
                if (view.selectedTab !== tab) CompositionWrites.writing { view.selectTab(tab) }
            }
        }
        onDispose { }
    }
}

/**
 * The tab to select when [removed] goes: the next one in declared order that still exists, or failing
 * that the previous one. Null when none is left.
 */
internal fun neighbourOf(removed: Any, order: List<Any>, remaining: Map<Any, *>): Any? {
    val at = order.indexOf(removed)
    if (at < 0) return remaining.keys.firstOrNull()
    for (i in at + 1 until order.size) if (order[i] in remaining) return order[i]
    for (i in at - 1 downTo 0) if (order[i] in remaining) return order[i]
    return null
}
