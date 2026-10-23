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
 * selection, and everything each tab's content remembered, stays put.
 *
 * @param selectedKey which tab to show. Leave null to let the widget manage its own selection.
 * @param onSelect notified when the user picks a tab, with that tab's key.
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
    val view = remember { LdlTabView() }
    // Tab element -> key, so a selection coming back out of the widget can be reported as the key
    // the caller declared rather than as an element they have never seen.
    val keysByTab = remember { HashMap<LdlTab, Any>() }
    val tabsByKey = remember { HashMap<Any, LdlTab>() }
    val currentOnSelect by rememberUpdatedState(onSelect)
    // Where a removed tab's rememberSaveable state waits, in case the tab comes back. Also what keeps
    // the tabs' saved state apart: each one saves under its own key.
    val tabStates = rememberSaveableStateHolder()

    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible,
    )

    DisposableEffect(view) {
        view.setOnTabSelected { tab -> keysByTab[tab]?.let { currentOnSelect?.invoke(it) } }
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
                view.addTab(tab, host)
                onDispose {
                    view.removeTab(tab)
                    keysByTab.remove(tab)
                    tabsByKey.remove(spec.key)
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

    // After the tabs exist, for the same reason as the selection below. `addTab` appends, and a tab
    // that only moved never passes through it again, so the declared order is imposed from here.
    val order = specs.map { it.key }
    DisposableEffect(order) {
        val headers = order.mapNotNull { tabsByKey[it] }
        val owned = headers.toHashSet()
        val container = view.tabScroller.viewContainer
        // Headers this composable did not put there keep their slots. Null means one of ours has
        // been moved out of the scroller, which leaves nothing sensible to reorder.
        spliceOrder(container.children, headers) { it in owned }?.let { container.reorderChildren(it) }
        onDispose { }
    }

    // After the tabs exist, not before: selecting a key that has not been added yet does nothing.
    DisposableEffect(selectedKey, specs.size) {
        if (selectedKey != null) {
            tabsByKey[selectedKey]?.let { if (view.selectedTab !== it) view.selectTab(it) }
        }
        onDispose { }
    }
}
