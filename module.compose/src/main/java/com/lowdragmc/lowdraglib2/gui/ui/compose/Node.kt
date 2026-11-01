package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Updater
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import com.lowdragmc.lowdraglib2.gui.ui.UIElement

/**
 * Emits any [UIElement] subclass as a composition node. Every wrapper here is a thin call to this,
 * and it is the escape hatch for a widget with no wrapper yet, or one from another mod.
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
 * @param classes space-separated stylesheet classes, so `.name { }` rules match. Diffed, so a name
 *   dropped from the string is removed again — which a style block, being written through, is not.
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
    widgetStyle: (T.() -> Unit)? = null,
    update: Updater<T>.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val observer = LocalNodeObserver.current
    val applied = remember { AppliedProps<T>() }
    ComposeNode<T, UIApplier>(
        factory = factory,
        update = {
            // reconcile, not set: the blocks are fresh lambdas every recomposition, and re-running
            // them is cheap since an unchanged style write returns early without dirtying anything.
            reconcile {
                applied.apply(
                    observer, this, layout, style, widgetStyle,
                    classes, id, enabled, visible, focusable, events,
                )
            }
            update()
        },
        content = content,
    )
}

/**
 * Mounts an element that already exists, built by hand or by the builder DSL.
 *
 * <p>The element is adopted as-is: the composition attaches, moves and detaches it, but never touches
 * its properties.
 *
 * <p>Passing a different element detaches the old one and attaches the new one in its place, so pass
 * a stable one (from `remember`): a fresh one every recomposition is swapped in every time, losing
 * whatever state it held. Emitted again later, the element first loses the children [content] gave it
 * last time.
 *
 * <p>Client only, like the rest of the composition: an element with sync values, RPCs or menu slots
 * has to be added to the tree in `createUI`, outside the [ComposeElement], for the server to have it.
 */
@Composable
fun Mount(element: UIElement, content: @Composable () -> Unit = {}) {
    // A node's factory only runs when it is created, so without the key a new element would never
    // replace the first.
    key(element) {
        ComposeNode<UIElement, UIApplier>(factory = { element }, update = {}, content = content)
    }
}

/**
 * Composes [content] into [container] rather than where the surrounding composition is emitting,
 * for widgets with named slots such as the two panes of a
 * [com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView]. Emits nothing where it is called.
 *
 * <p>A subcomposition of the caller: `CompositionLocal`s reach it, it recomposes on the same frames,
 * and it is torn down with its parent.
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
    // Read inside the child, so a new lambda invalidates the child without another setContent.
    val current by rememberUpdatedState(content)
    val composition = remember(container, parent) { Composition(UIApplier(container), parent) }
    // DisposableEffect, not SideEffect: every DisposableEffect runs before any SideEffect, so the
    // container is filled before later effects run - a Dialog shown on its first frame inspects it.
    DisposableEffect(composition) {
        composition.setContent { current() }
        onDispose { composition.dispose() }
    }
}
