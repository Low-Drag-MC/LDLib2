package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Updater
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import com.lowdragmc.lowdraglib2.gui.ui.UIElement

/**
 * Emits any [UIElement] subclass as a composition node.
 *
 * <p>Every wrapper in this package is a thin call to this, and it is also the escape hatch: a widget
 * with no wrapper yet, or one from another mod, can be used immediately.
 *
 * ```kotlin
 * UIElementNode(
 *     factory = { ProgressBar() },
 *     layout = { size(80.px, 10.px) },
 *     update = { set(progress) { setProgress(it) } },
 * )
 * ```
 *
 * @param factory builds the element. Runs once, when the node is first created — never on
 *   recomposition, so anything that varies belongs in [update], not here.
 * @param layout the element's layout, with the same receiver `layoutDsl { }` gives you.
 * @param style the element's appearance, with the same receiver `styleDsl { }` gives you.
 * @param classes space-separated stylesheet classes, so `.name { }` rules match. This is the right
 *   place for appearance that varies with state — see [AppliedProps] for why a style block is not.
 * @param update applies props. Wrap each one in [Updater.set] so it only runs when that prop
 *   actually changed.
 */
@Composable
fun <T : UIElement> UIElementNode(
    factory: () -> T,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    update: Updater<T>.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    // Shares the node's lifetime, so there is nothing to clean up when the node goes away.
    val applied = remember { AppliedProps() }
    ComposeNode<T, UIApplier>(
        factory = factory,
        update = {
            // reconcile, not set: the blocks are fresh lambdas on every recomposition, so there is
            // nothing meaningful to compare them against. Running them again is cheap — a style
            // write whose value is unchanged returns early in StyleBag#replaceOrPutCandidate without
            // dirtying anything — and classes and events are diffed properly inside.
            reconcile {
                applied.apply(this, layout, style, classes, id, enabled, visible, focusable, events)
            }
            update()
        },
        content = content,
    )
}

/**
 * Mounts an element that already exists, built by hand or by the builder DSL.
 *
 * <p>Useful for dropping a configured widget into a declarative tree without writing a wrapper for
 * it. The element is adopted as-is: the composition will attach, move and detach it, but never
 * touches its properties, so whoever built it stays in charge of what it looks like.
 *
 * <p>[element] must be stable across recompositions — pass one from `remember`. Handing over a
 * freshly constructed element on every recomposition would replace the node each time and throw away
 * whatever state it held.
 */
@Composable
fun Mount(element: UIElement, content: @Composable () -> Unit = {}) {
    ComposeNode<UIElement, UIApplier>(factory = { element }, update = {}, content = content)
}

/**
 * Composes [content] into an element that is not where the surrounding composition is emitting.
 *
 * <p>This is what makes widgets with named slots work. A [com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView]
 * does not have one child list to fill — it has two panes — and an applier only ever knows one place
 * to put things. So each pane gets its own composition, pointed at that pane.
 *
 * <p>It is a real subcomposition, not an independent one: [rememberCompositionContext] links it to
 * the composition it is called from, so `CompositionLocal`s reach it, it recomposes on the same
 * frames, and it is torn down with its parent. Emits nothing where it is called.
 *
 * ```kotlin
 * val view = remember { SplitView.Horizontal() }
 * UIElementNode(factory = { view }, layout = { … })
 * Subcompose(view.first) { LeftPane() }
 * Subcompose(view.second) { RightPane() }
 * ```
 */
@Composable
fun Subcompose(container: UIElement, content: @Composable () -> Unit) {
    val parent = rememberCompositionContext()
    val composition = remember(container, parent) { Composition(UIApplier(container), parent) }
    DisposableEffect(composition) { onDispose { composition.dispose() } }
    // Re-set every recomposition: `content` is a fresh lambda each time, capturing whatever state
    // the caller read, and the child composition has no other way to learn about it.
    SideEffect { composition.setContent(content) }
}
