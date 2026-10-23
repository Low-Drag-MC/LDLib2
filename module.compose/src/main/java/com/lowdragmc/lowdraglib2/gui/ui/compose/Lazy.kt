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
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyLayoutStyleDsl
import com.lowdragmc.lowdraglib2.gui.ui.layout.pct
import com.lowdragmc.lowdraglib2.gui.ui.layout.px
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import kotlin.math.abs
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
 * <p>Rows are all [itemHeight] tall unless [variableHeight] is set, in which case they measure
 * themselves and [itemHeight] is only the opening guess.
 *
 * @param itemHeight the height of every row in pixels — or, with [variableHeight], the estimate used
 *   for rows that have not been on screen yet.
 * @param variableHeight lets rows be different heights, at the cost of a wrapper element per row and
 *   a settling frame. See below for what that costs and when it is wrong.
 * @param overscan how many extra rows to keep mounted beyond each edge, so a fast scroll does not
 *   show a gap before the next frame catches up.
 * @param key identifies a row across scrolls and list edits. Defaults to the index, which is right
 *   for a list that only ever grows at the end and wrong for one that gets items inserted.
 */
@Composable
fun <T> LazyColumn(
    items: List<T>,
    itemHeight: Float,
    variableHeight: Boolean = false,
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
    LazyList(
        vertical = true, items = items, itemSize = itemHeight, variableSize = variableHeight,
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        overscan = overscan, key = key, itemContent = itemContent,
    )
}

/**
 * The machinery behind [LazyColumn] and [LazyRow], which differ only in the axis.
 *
 * <p>With [variableSize] off the window is plain arithmetic on [itemSize]. With it on, each item is
 * wrapped so it can report its extent, and the window is worked out from what has been measured so
 * far — see [MeasuredItem] for what that costs.
 */
@Composable
private fun <T> LazyList(
    vertical: Boolean,
    items: List<T>,
    itemSize: Float,
    variableSize: Boolean,
    layout: LayoutBlock?,
    style: StyleBlock?,
    classes: String?,
    id: String?,
    enabled: Boolean,
    visible: Boolean,
    focusable: Boolean?,
    events: (EventScope.() -> Unit)?,
    overscan: Int,
    key: ((index: Int, item: T) -> Any)?,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    val view = remember { ScrollerView() }
    val metrics = if (vertical) {
        rememberScrollMetrics(view, view.verticalScroller) { view.viewPort.contentHeight }
    } else {
        rememberScrollMetrics(view, view.horizontalScroller) { view.viewPort.contentWidth }
    }
    val total = items.size

    val sizes: ItemSizes?
    val range: IntRange
    val leading: Float
    val trailing: Float
    if (variableSize) {
        sizes = remember(itemSize) { ItemSizes(itemSize) }
        sizes.resize(total)
        // The subscription. `sizes` is a plain array behind it — see ItemSizes for why.
        @Suppress("UNUSED_EXPRESSION") sizes.version
        val window = measuredWindow(sizes.sizes, metrics.viewport, metrics.fraction, overscan)
        range = window.range
        leading = window.leading
        trailing = window.trailing
    } else {
        sizes = null
        range = lazyWindow(total, itemSize, metrics.viewport, metrics.fraction, overscan)
        // An empty list has an empty range, whose `first` is 1 rather than 0.
        leading = if (range.isEmpty()) 0f else range.first * itemSize
        trailing = if (range.isEmpty()) 0f else (total - range.last - 1) * itemSize
    }

    UIElementNode(
        // The row direction goes on the *content* container, not on the ScrollerView. Putting it on
        // the view itself lays out the view's own parts — the viewport and its scrollbars — side by
        // side, and leaves the items still stacked vertically.
        factory = {
            if (!vertical) view.viewContainer.getLayout().flexDirection(FlexDirection.ROW)
            view
        },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    ) {
        AxisSpacer(vertical, leading)
        for (index in range) {
            val item = items[index]
            key(key?.invoke(index, item) ?: index) {
                if (sizes != null) {
                    MeasuredItem(sizes, index, vertical) { itemContent(index, item) }
                } else {
                    itemContent(index, item)
                }
            }
        }
        AxisSpacer(vertical, trailing)
    }
}

/**
 * Wraps one variable-size item so it can report how big it turned out to be.
 *
 * <p>An item can only be measured once it has been laid out, which is a frame after it was composed —
 * so the first frame an item appears it is still occupying its estimate, and the window corrects on
 * the next. That settling is visible as a small scrollbar adjustment when you scroll into fresh
 * items, and it is the price of not knowing sizes up front.
 *
 * <p>Fills the cross axis, extent along the scrolling axis left to the content. An item whose content
 * does not size itself measures zero, which [ItemSizes.measure] refuses — otherwise a list of
 * zero-size items would decide the whole list fits on screen and mount all of it. Across a row the
 * wrapper also refuses to shrink: the strip is laid out as a flex row, which would otherwise squeeze
 * unsized items to fit the viewport before they were ever measured.
 */
@Composable
private fun MeasuredItem(sizes: ItemSizes, index: Int, vertical: Boolean, content: @Composable () -> Unit) {
    Box(
        layout = {
            if (vertical) {
                width(100.pct)
            } else {
                height(100.pct)
                flexShrink(0)
            }
        },
        events = {
            on(UIEvents.LAYOUT_CHANGED) { event ->
                event.currentElement?.let { sizes.measure(index, if (vertical) it.sizeHeight else it.sizeWidth) }
            }
        },
        content = content,
    )
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

/** Where a variable-size window sits, and how much blank space to leave on either side of it. */
internal data class MeasuredWindow(
    val range: IntRange,
    val leading: Float,
    val trailing: Float,
)

/**
 * The same question as [lazyWindow], for items that are not all the same size. Named for the
 * vertical case; [LazyRow] hands it widths.
 *
 * <p>Takes the heights rather than measuring anything, so it stays pure and testable — and because
 * the heights it is given are a mixture of measured and estimated by nature. A row that has never
 * been on screen has never been laid out, so its height is a guess until it has been; the window this
 * returns is correct for the numbers it was handed, and self-corrects as guesses are replaced.
 *
 * <p>`leading + (heights over range) + trailing` always equals the total, which is what keeps the
 * scrollbar honest: the spacers plus the mounted rows add up to the whole list.
 *
 * @param heights one entry per item, measured where known and estimated where not.
 */
internal fun measuredWindow(
    heights: FloatArray,
    viewportSize: Float,
    scrollFraction: Float,
    overscan: Int,
): MeasuredWindow {
    val total = heights.size
    if (total == 0) return MeasuredWindow(IntRange.EMPTY, 0f, 0f)

    var content = 0f
    for (height in heights) content += height

    val maxScroll = (content - viewportSize).coerceAtLeast(0f)
    val scrolled = scrollFraction.coerceIn(0f, 1f) * maxScroll

    // First row whose bottom edge is past the scroll position, tracking its offset as we go.
    var first = 0
    var firstOffset = 0f
    while (first < total - 1 && firstOffset + heights[first] <= scrolled) {
        firstOffset += heights[first]
        first++
    }

    // Then forward until the viewport is covered. Zero viewport means the first layout has not
    // happened yet; one row is enough to get something on screen and the next frame corrects it.
    var last = first
    var filled = firstOffset + heights[first]
    while (last < total - 1 && filled < scrolled + viewportSize) {
        last++
        filled += heights[last]
    }

    val start = (first - overscan).coerceAtLeast(0)
    val end = (last + overscan).coerceAtMost(total - 1)

    var leading = firstOffset
    for (i in start until first) leading -= heights[i]
    var mounted = 0f
    for (i in start..end) mounted += heights[i]

    return MeasuredWindow(start..end, leading, (content - leading - mounted).coerceAtLeast(0f))
}

/**
 * The extents of a list's items along its scrolling axis, measured where they have been on screen
 * and estimated where not.
 *
 * <p>[version] rather than the array itself is the observable state. An item reports its size from a
 * layout event, and copying a ten-thousand-entry array on each of those — several per frame while
 * scrolling — costs more than the virtualisation saves. So the array is mutated in place and the
 * counter is bumped, which is what readers actually subscribe to.
 */
@Stable
internal class ItemSizes(private val estimate: Float) {
    private var array = FloatArray(0)

    var version by mutableStateOf(0)
        private set

    /** Read [version] first — that is the subscription; this is just the data. */
    val sizes: FloatArray get() = array

    fun resize(count: Int) {
        val old = array
        if (old.size == count) return
        array = FloatArray(count) { if (it < old.size) old[it] else estimate }
        version++
    }

    /**
     * Records a measured size.
     *
     * <p>Ignores changes under half a pixel. Layout produces fractional values, and an item whose
     * size wobbles in the noise would invalidate the window every frame, which re-lays-out the
     * items, which produces another wobble.
     */
    fun measure(index: Int, size: Float) {
        val old = array
        if (index !in old.indices || size <= 0f) return
        if (abs(old[index] - size) < 0.5f) return
        old[index] = size
        version++
    }
}

/**
 * A horizontally scrolling equivalent of [LazyColumn].
 *
 * <p>Same machinery on the other axis: items are all [itemWidth] wide unless [variableWidth] is set,
 * in which case they measure themselves and [itemWidth] is only the opening guess. The costs of
 * measuring are the ones [LazyColumn] describes.
 */
@Composable
fun <T> LazyRow(
    items: List<T>,
    itemWidth: Float,
    variableWidth: Boolean = false,
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
    LazyList(
        vertical = false, items = items, itemSize = itemWidth, variableSize = variableWidth,
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        overscan = overscan, key = key, itemContent = itemContent,
    )
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
 *
 * <p>Cells are [cellWidth] by [cellHeight] unless [variableHeight] is set. Then each row is as tall as
 * its tallest cell, every cell in it stretches to match, and [cellHeight] is only the estimate for
 * rows that have not been on screen — the same trade [LazyColumn] makes. Widths stay fixed either
 * way: the columns have to line up.
 */
@Composable
fun <T> LazyVerticalGrid(
    items: List<T>,
    columns: Int,
    cellWidth: Float,
    cellHeight: Float,
    variableHeight: Boolean = false,
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
        variableHeight = variableHeight,
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        overscan = overscan,
        key = { _, row -> row },
    ) { _, row ->
        // Row aligns its children to the top by default; a variable row has to stretch them instead,
        // or a short cell stops short of its taller neighbours.
        Row(layout = { if (variableHeight) alignItems(AlignItems.STRETCH) else height(cellHeight.px) }) {
            for (column in 0 until columns) {
                val index = row * columns + column
                if (index < items.size) {
                    val item = items[index]
                    key(key?.invoke(index, item) ?: index) {
                        Box(layout = { cell(cellWidth, cellHeight, variableHeight) }) {
                            itemContent(index, item)
                        }
                    }
                } else {
                    // Padding out the last row, so its cells sit under the ones above.
                    Box(layout = { cell(cellWidth, cellHeight, variableHeight) })
                }
            }
        }
    }
}

private fun TaffyLayoutStyleDsl.cell(cellWidth: Float, cellHeight: Float, variableHeight: Boolean) {
    width(cellWidth.px)
    if (!variableHeight) height(cellHeight.px)
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
private fun AxisSpacer(vertical: Boolean, extent: Float) {
    val px = extent.coerceAtLeast(0f).px
    Spacer(layout = { if (vertical) height(px) else width(px) })
}
