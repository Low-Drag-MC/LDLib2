package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
     *   something stable, not the index.
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
 * <p>Tabs appearing and disappearing is handled; **reordering an existing set is not**. The headers
 * keep the order they were added in, because a tab's element is attached once, when it enters the
 * composition, and nothing re-attaches it afterwards. Adding and removing is the case worth having,
 * and the case that would otherwise leak elements.
 *
 * @param selectedKey which tab to show. Leave null to let the widget manage its own selection.
 * @param onSelect notified when the user picks a tab, with that tab's key.
 */
@Composable
fun TabView(
    modifier: UIModifier = UIModifier,
    selectedKey: Any? = null,
    onSelect: ((Any) -> Unit)? = null,
    content: TabScope.() -> Unit,
) {
    val specs = TabScope().apply(content).specs
    val view = remember { LdlTabView() }
    // Tab element -> key, so a selection coming back out of the widget can be reported as the key
    // the caller declared rather than as an element they have never seen.
    val keysByTab = remember { HashMap<LdlTab, Any>() }
    val tabsByKey = remember { HashMap<Any, LdlTab>() }
    val currentOnSelect by rememberUpdatedState(onSelect)

    UIElementNode(factory = { view }, modifier = modifier)

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
            Subcompose(host, spec.content)
        }
    }

    // After the tabs exist, not before: selecting a key that has not been added yet does nothing.
    DisposableEffect(selectedKey, specs.size) {
        if (selectedKey != null) {
            tabsByKey[selectedKey]?.let { if (view.selectedTab !== it) view.selectTab(it) }
        }
        onDispose { }
    }
}
