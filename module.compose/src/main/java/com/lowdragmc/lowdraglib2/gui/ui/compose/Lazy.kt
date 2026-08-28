package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import dev.vfyjxf.taffy.style.FlexDirection
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A scrolling list that only composes the rows you can see.
 *
 * ```kotlin
 * LazyColumn(items = recipes, itemHeight = 18f, key = { _, r -> r.id }) { _, recipe ->
 *     RecipeRow(recipe)
 * }
 * ```
 *
 * <p>A thousand-row list composed eagerly is a thousand elements, each with a layout node and a
 * stylesheet match — several megabytes and a visible hitch. This keeps roughly a screenful mounted
 * and represents everything above and below as two empty spacer elements, so the scrollbar still
 * behaves as though the whole list were there.
 *
 * <p>Rows enter and leave as you scroll, which means a row's `remember`ed state does not survive
 * being scrolled out of view. Anything that must outlive that — a selection, an expanded flag, an
 * edit in progress — belongs in state held above this call, keyed by item.
 *
 * <p>Rows must all be [itemHeight] tall. Measuring them instead would mean composing them to find
 * out how big they are, which is the thing being avoided; a variable-height version needs a measured
 * height cache and is not implemented.
 *
 * @param itemHeight the fixed height of every row, in pixels.
 * @param overscan how many extra rows to keep mounted beyond each edge, so a fast scroll does not
 *   show a gap before the next frame catches up.
 * @param key identifies a row across scrolls and list edits. Defaults to the index, which is right
 *   for a list that only ever grows at the end and wrong for one that gets items inserted.
 */
@Composable
fun <T> LazyColumn(
    items: List<T>,
    itemHeight: Float,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    overscan: Int = 2,
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    require(itemHeight > 0f) { "itemHeight must be positive, was $itemHeight" }

    val view = remember { ScrollerView() }
    val metrics = rememberScrollMetrics(view, view.verticalScroller) { view.viewPort.contentHeight }

    val total = items.size
    val window = lazyWindow(total, itemHeight, metrics.viewport, metrics.fraction, overscan)

    UIElementNode(
        factory = { view },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    ) {
        Spacer(window.first * itemHeight)
        for (index in window) {
            val item = items[index]
            key(key?.invoke(index, item) ?: index) {
                itemContent(index, item)
            }
        }
        Spacer((total - window.last - 1) * itemHeight)
    }
}

/** Where the scrollbar is and how much fits, along one axis. */
@Stable
private class ScrollMetrics {
    var fraction by mutableStateOf(0f)
    var viewport by mutableStateOf(0f)
}

/**
 * Pulls a scroller's position and viewport extent into composition state.
 *
 * <p>Both arrive from the element tree rather than from the composition, so they have to be
 * subscribed to. [viewportExtent] is a lambda because the axis differs — content height for a
 * column, width for a row — and everything else about the two cases is identical.
 */
@Composable
private fun rememberScrollMetrics(
    view: ScrollerView,
    scroller: Scroller,
    viewportExtent: () -> Float,
): ScrollMetrics {
    val metrics = remember(view) { ScrollMetrics() }
    DisposableEffect(view, scroller) {
        // registerValueListener, not setOnValueChanged: the latter is how ScrollerView moves its own
        // content, and taking it over would stop the list scrolling at all.
        val subscription = scroller.registerValueListener { value -> metrics.fraction = value ?: 0f }
        val onLayout = UIEventListener { metrics.viewport = viewportExtent() }
        view.viewPort.addEventListener(UIEvents.LAYOUT_CHANGED, onLayout)
        metrics.viewport = viewportExtent()
        onDispose {
            subscription.unsubscribe()
            view.viewPort.removeEventListener(UIEvents.LAYOUT_CHANGED, onLayout)
        }
    }
    return metrics
}

/**
 * Which slice of a fixed-size list is worth having mounted.
 *
 * <p>Pure, and separate from the composables, so it can be tested without a UI — `src/test` has no
 * Minecraft on its classpath, and this arithmetic is the part that is easy to get subtly wrong at the
 * ends of the range.
 *
 * @param viewportSize the visible extent along the scrolling axis. Zero before the first layout,
 *   which is why that case has to produce a usable window rather than an empty one.
 * @return the indices to mount, empty when there is nothing to show.
 */
internal fun lazyWindow(
    total: Int,
    itemSize: Float,
    viewportSize: Float,
    scrollFraction: Float,
    overscan: Int,
): IntRange {
    if (total <= 0) return IntRange.EMPTY
    // Before the first layout the viewport is still zero; mounting one item plus the overscan is
    // enough to get something on screen, and the next frame corrects it.
    val fits = if (viewportSize > 0f) ceil(viewportSize / itemSize).toInt() + 1 else 1
    val maxScroll = (total * itemSize - viewportSize).coerceAtLeast(0f)
    val scrolledPx = scrollFraction.coerceIn(0f, 1f) * maxScroll

    val first = (floor(scrolledPx / itemSize).toInt() - overscan).coerceIn(0, total - 1)
    val last = (first + fits + overscan * 2).coerceAtMost(total) - 1
    return first..last
}

/**
 * A horizontally scrolling equivalent of [LazyColumn].
 *
 * <p>Same machinery on the other axis, and the same fixed-size requirement — see [LazyColumn] for
 * why measuring instead is not an option.
 */
@Composable
fun <T> LazyRow(
    items: List<T>,
    itemWidth: Float,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    overscan: Int = 2,
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    require(itemWidth > 0f) { "itemWidth must be positive, was $itemWidth" }

    val view = remember { ScrollerView() }
    val metrics = rememberScrollMetrics(view, view.horizontalScroller) { view.viewPort.contentWidth }

    val total = items.size
    val window = lazyWindow(total, itemWidth, metrics.viewport, metrics.fraction, overscan)

    UIElementNode(
        // The row direction goes on the *content* container, not on the ScrollerView. Putting it on
        // the view itself lays out the view's own parts — the viewport and its scrollbars — side by
        // side, and leaves the items still stacked vertically.
        factory = { view.also { it.viewContainer.getLayout().flexDirection(FlexDirection.ROW) } },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    ) {
        HorizontalSpacer(window.first * itemWidth)
        for (index in window) {
            val item = items[index]
            key(key?.invoke(index, item) ?: index) { itemContent(index, item) }
        }
        HorizontalSpacer((total - window.last - 1) * itemWidth)
    }
}

/**
 * A scrolling grid that only composes the rows you can see.
 *
 * ```kotlin
 * LazyVerticalGrid(items = stacks, columns = 9, cellWidth = 18f, cellHeight = 18f) { _, stack ->
 *     ItemSlot(item = stack)
 * }
 * ```
 *
 * <p>The unit of virtualisation is the *row*, not the cell: whole rows of [columns] cells go in and
 * out together, which is what keeps the arithmetic the same as [LazyColumn]'s. Inventories and
 * recipe tables are the obvious users.
 *
 * <p>A trailing partial row is padded with empty cells, so the last row lines up with the ones above
 * rather than centring or stretching.
 */
@Composable
fun <T> LazyVerticalGrid(
    items: List<T>,
    columns: Int,
    cellWidth: Float,
    cellHeight: Float,
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
    id: String? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    focusable: Boolean? = null,
    events: (EventScope.() -> Unit)? = null,
    overscan: Int = 1,
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    require(columns > 0) { "columns must be positive, was $columns" }
    require(cellWidth > 0f && cellHeight > 0f) { "cell size must be positive" }

    val rowCount = (items.size + columns - 1) / columns
    val rows = remember(items, columns) { (0 until rowCount).toList() }

    LazyColumn(
        items = rows,
        itemHeight = cellHeight,
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        overscan = overscan,
        key = { _, row -> row },
    ) { _, row ->
        Row(layout = { height(cellHeight.px) }) {
            for (column in 0 until columns) {
                val index = row * columns + column
                if (index < items.size) {
                    val item = items[index]
                    key(key?.invoke(index, item) ?: index) {
                        Box(layout = { size(cellWidth.px, cellHeight.px) }) {
                            itemContent(index, item)
                        }
                    }
                } else {
                    // Padding out the last row, so its cells sit under the ones above.
                    Box(layout = { size(cellWidth.px, cellHeight.px) })
                }
            }
        }
    }
}

/**
 * An empty element of a fixed size.
 *
 * <p>Inside the lazy containers it stands in for the items that are not mounted, which is what keeps
 * the scrollbar honest — the content still adds up to the whole list, so the thumb is the right size
 * and lands in the right place. It is public because it is just as useful for putting a gap between
 * two things.
 */
@Composable
fun Spacer(
    layout: LayoutBlock? = null,
    style: StyleBlock? = null,
    classes: String? = null,
) {
    UIElementNode(factory = { UIElement() }, layout = layout, style = style, classes = classes)
}

@Composable
private fun Spacer(extent: Float) =
    Spacer(layout = { height(extent.coerceAtLeast(0f).px) })

@Composable
private fun HorizontalSpacer(extent: Float) =
    Spacer(layout = { width(extent.coerceAtLeast(0f).px) })
