package com.lowdragmc.lowdraglib2.gui.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode
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
 * <p>Keeps roughly a screenful mounted and stands in for everything above and below with two empty
 * spacers, so the scrollbar behaves as though the whole list were there.
 *
 * <p>Rows enter and leave as you scroll, which means a row's `remember`ed state does not survive
 * being scrolled out of view. `rememberSaveable` state does: each row saves under its [key] on the way
 * out and gets it back on the way in, and that saved state is dropped once its key is no longer in
 * [items]. Shared state — a selection across rows — still belongs above this call.
 *
 * <p>When [items] changes, or a variable-height row turns out a different height, the list stays on
 * the row it was showing: the first visible row keeps its place on screen, wherever it moved to in the
 * list. If that row was removed, the row that took its place does.
 *
 * <p>[items] is compared by identity to notice a change. Hand over a new list, or a snapshot state
 * list whose size changes. Replacing one element of a snapshot state list in place shows the new
 * element at once, but under the old element's key, saved state and measured size.
 *
 * @param itemHeight with [variableHeight] off, the height every row *is* — nothing enforces it, so
 *   give each row that height (`layout = { height(itemHeight.px) }`) or the spacers and the rows will
 *   disagree. With [variableHeight] on, only the estimate for rows not yet on screen.
 * @param variableHeight lets rows be different heights, at the cost of a wrapper element per row and
 *   a settling frame: a row is measured once laid out, so it occupies the estimate on its first frame.
 * @param overscan how many extra rows to keep mounted beyond each edge, so a fast scroll does not
 *   show a gap before the next frame catches up.
 * @param key identifies a row across scrolls and list edits. Defaults to the index, which is right
 *   for a list that only ever grows at the end and wrong for one that gets items inserted. Keys should
 *   be unique; repeated ones are told apart by the order they appear in.
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
 * A horizontally scrolling equivalent of [LazyColumn].
 *
 * <p>Same machinery on the other axis: items are all [itemWidth] wide unless [variableWidth] is set,
 * in which case they measure themselves and [itemWidth] is only the opening guess. Everything
 * [LazyColumn] says about keys, saved state and staying in place applies here too.
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

/** Shared by [LazyColumn], [LazyRow] and [LazyVerticalGrid] (whose items are its rows). */
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
    /** Where to go when the row the list was showing has gone: its old key and index to a new index. */
    remapAnchor: ((key: Any, index: Int) -> Int)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    require(overscan >= 0) { "overscan must not be negative, was $overscan" }
    val view = remember { ScrollerView() }
    val scroller = if (vertical) view.verticalScroller else view.horizontalScroller
    val metrics = rememberScrollMetrics(view, scroller) {
        if (vertical) view.viewPort.contentHeight else view.viewPort.contentWidth
    }
    val itemStates = rememberSaveableStateHolder()
    // Saveable too, so state saved under a key deleted while the screen was closed is still pruned.
    val everMounted = rememberSaveable { HashSet<Any>() }
    val keys = remember(Identity(items), items.size, key != null) { ItemKeys(itemKeys(items, key)) }
    val total = keys.size

    val sizes = if (variableSize) remember(itemSize) { ItemSizes(itemSize) } else null
    if (sizes != null) remember(keys, sizes) { sizes.lineUp(keys.keys) }
    // Reading version is the subscription; the array behind `sizes` is not observable.
    val sizesVersion = sizes?.version ?: 0
    val extentOf: (Int) -> Float = if (sizes != null) { i -> sizes.sizes[i] } else { _ -> itemSize }
    val viewport = metrics.viewport.takeIf { it.isFinite() && it > 0f } ?: 0f
    var fraction = metrics.fraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f

    val anchor = remember { ScrollAnchor() }
    val listChanged = anchor.keys !== keys
    // A measurement only re-anchors when nothing else moved the list - a scroll in the same frame wins -
    // and not at the end, where the list stays at the end.
    val sizesChanged = anchor.sizesVersion != sizesVersion && fraction == anchor.fraction && fraction < 0.9999f
    if ((listChanged || sizesChanged) && anchor.keys != null && viewport > 0f && total > 0) {
        val previous = anchor.key
        val found = previous?.let(keys::indexOf) ?: -1
        val at = when {
            found >= 0 -> found
            previous != null && remapAnchor != null -> remapAnchor(previous, anchor.index)
            else -> anchor.index
        }.coerceAtMost(total - 1)
        if (at >= 0) {
            var start = 0f
            var content = 0f
            for (i in 0 until total) {
                if (i == at) start = content
                content += extentOf(i)
            }
            val maxScroll = content - viewport
            if (maxScroll > 0f) {
                val target = ((start + anchor.offset.coerceAtMost(extentOf(at))) / maxScroll).coerceIn(0f, 1f)
                if (abs(target - fraction) * maxScroll > 0.5f) {
                    fraction = target
                    SideEffect { CompositionWrites.writing { scroller.setValue(target) } }
                }
            }
        }
    }
    anchor.keys = keys
    anchor.sizesVersion = sizesVersion
    anchor.fraction = fraction

    val window = if (sizes != null) {
        measuredWindow(sizes.sizes, viewport, fraction, overscan)
    } else {
        fixedWindow(total, itemSize, viewport, fraction, overscan)
    }
    if (window.firstVisible >= 0) {
        anchor.key = keys.keys[window.firstVisible]
        anchor.index = window.firstVisible
        anchor.offset = window.firstVisibleOffset
    }

    // After the composition that dropped them, so nothing still mounted is touched.
    DisposableEffect(keys) {
        everMounted.removeAll { stale -> (keys.indexOf(stale) < 0).also { gone -> if (gone) itemStates.removeState(stale) } }
        onDispose { }
    }

    UIElementNode(
        // The row direction goes on the content container: on the view itself it would lay out the
        // viewport and scrollbars side by side instead.
        factory = {
            if (!vertical) {
                view.viewContainer.getLayout().flexDirection(FlexDirection.ROW)
                // Otherwise the ordinary wheel goes to the vertical scroller, which has nothing to do.
                view.scrollerViewStyle.asDefault { mode(ScrollerMode.HORIZONTAL) }
            }
            view
        },
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
    ) {
        AxisSpacer(vertical, window.leading)
        for (index in window.range) {
            val item = items[index]
            val itemKey = keys.keys[index]
            everMounted.add(itemKey)
            key(itemKey) {
                itemStates.SaveableStateProvider(itemKey) {
                    if (sizes != null) {
                        MeasuredItem(sizes, itemKey, index, vertical) { itemContent(index, item) }
                    } else {
                        itemContent(index, item)
                    }
                }
            }
        }
        AxisSpacer(vertical, window.trailing)
    }
}

/** A `remember` key that compares by identity, where a list's own `equals` would walk every element. */
internal class Identity(private val value: Any?) {
    override fun equals(other: Any?) = other is Identity && other.value === value
    override fun hashCode() = System.identityHashCode(value)
}

/** Which row the list was showing, and what it was computed from, carried to the next composition. */
private class ScrollAnchor {
    var keys: ItemKeys? = null
    var sizesVersion = 0
    var fraction = 0f
    var key: Any? = null
    var index = -1
    var offset = 0f
}

/** A key that appeared more than once in a list, told apart by which occurrence it is. */
internal data class RepeatedKey(val key: Any, val occurrence: Int)

/**
 * The key of every item, the index when no [key] lambda is given. Repeats become [RepeatedKey]s: the
 * saved-state holder refuses a second registration under one key, which stops the whole composition.
 */
internal fun <T> itemKeys(items: List<T>, key: ((index: Int, item: T) -> Any)?): List<Any> {
    if (key == null) return List(items.size) { it }
    val seen = HashMap<Any, Int>()
    return items.mapIndexed { index, item ->
        val itemKey = key(index, item)
        val occurrence = seen.merge(itemKey, 1, Int::plus)!! - 1
        if (occurrence == 0) itemKey else RepeatedKey(itemKey, occurrence)
    }
}

/** The keys of one version of the list, with a lookup from key to position built on first use. */
internal class ItemKeys(val keys: List<Any>) {
    val size: Int get() = keys.size

    private val positions: HashMap<Any, Int> by lazy {
        HashMap<Any, Int>(keys.size * 2).also { map -> keys.forEachIndexed { i, k -> map[k] = i } }
    }

    fun indexOf(key: Any): Int = positions[key] ?: -1
}

/**
 * Wraps one variable-size item so it reports its extent along the scrolling axis; it fills the cross
 * axis. Content that does not size itself measures zero, which [ItemSizes.measure] ignores: a list of
 * zero-size items would otherwise seem to fit on screen and mount all of it.
 */
@Composable
private fun MeasuredItem(sizes: ItemSizes, key: Any, index: Int, vertical: Boolean, content: @Composable () -> Unit) {
    Box(
        layout = { if (vertical) width(100.pct) else height(100.pct) },
        events = {
            on(UIEvents.LAYOUT_CHANGED) { event ->
                event.currentElement?.let { sizes.measure(key, index, if (vertical) it.sizeHeight else it.sizeWidth) }
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

/** Pulls a scroller's position and viewport extent into composition state. */
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
 * Which slice of a list to mount, the blank space either side of it, and which item is at the top of
 * the viewport. `leading + (sizes over range) + trailing` always equals the total.
 *
 * @property firstVisible the item at the start of the viewport, or -1 for an empty list.
 * @property firstVisibleOffset how far into that item the viewport starts.
 */
internal data class LazyWindow(
    val range: IntRange,
    val leading: Float,
    val trailing: Float,
    val firstVisible: Int,
    val firstVisibleOffset: Float,
)

private val EMPTY_WINDOW = LazyWindow(IntRange.EMPTY, 0f, 0f, -1, 0f)

/**
 * [measuredWindow] for items that are all [itemSize], without an array of sizes.
 *
 * @param viewportSize the visible extent along the scrolling axis. Zero before the first layout, which
 *   still mounts one item plus the overscan; the next frame corrects it.
 */
internal fun fixedWindow(
    total: Int,
    itemSize: Float,
    viewportSize: Float,
    scrollFraction: Float,
    overscan: Int,
): LazyWindow {
    if (total <= 0) return EMPTY_WINDOW
    val fits = if (viewportSize > 0f) ceil(viewportSize / itemSize).toInt() + 1 else 1
    val maxScroll = (total * itemSize - viewportSize).coerceAtLeast(0f)
    val scrolled = scrollFraction.coerceIn(0f, 1f) * maxScroll

    val visible = floor(scrolled / itemSize).toInt().coerceIn(0, total - 1)
    val first = (visible - overscan).coerceAtLeast(0)
    val last = (first + fits + overscan * 2).coerceAtMost(total) - 1
    return LazyWindow(
        first..last,
        leading = first * itemSize,
        trailing = (total - last - 1) * itemSize,
        firstVisible = visible,
        firstVisibleOffset = scrolled - visible * itemSize,
    )
}

/**
 * The same question as [fixedWindow], for items that are not all the same size.
 *
 * @param sizes one entry per item along the scrolling axis, measured where known and estimated
 *   where not.
 */
internal fun measuredWindow(
    sizes: FloatArray,
    viewportSize: Float,
    scrollFraction: Float,
    overscan: Int,
): LazyWindow {
    val total = sizes.size
    if (total == 0) return EMPTY_WINDOW

    var content = 0f
    for (size in sizes) content += size

    val maxScroll = (content - viewportSize).coerceAtLeast(0f)
    val scrolled = scrollFraction.coerceIn(0f, 1f) * maxScroll

    var first = 0
    var firstOffset = 0f
    while (first < total - 1 && firstOffset + sizes[first] <= scrolled) {
        firstOffset += sizes[first]
        first++
    }

    // A zero viewport (before the first layout) covers just the first item; the next frame corrects it.
    var last = first
    var filled = firstOffset + sizes[first]
    while (last < total - 1 && filled < scrolled + viewportSize) {
        last++
        filled += sizes[last]
    }

    val start = (first - overscan).coerceAtLeast(0)
    val end = (last + overscan).coerceAtMost(total - 1)

    var leading = firstOffset
    for (i in start until first) leading -= sizes[i]
    var mounted = 0f
    for (i in start..end) mounted += sizes[i]

    return LazyWindow(
        start..end,
        leading,
        (content - leading - mounted).coerceAtLeast(0f),
        firstVisible = first,
        firstVisibleOffset = scrolled - firstOffset,
    )
}

/**
 * The extents of a list's items along its scrolling axis, measured where they have been on screen and
 * estimated where not. Kept by item key, so measurements move with their items.
 *
 * <p>[version], not the array, is the observable state: copying a large array on every layout event
 * costs more than virtualisation saves, so it is mutated in place and the counter bumped.
 */
@Stable
internal class ItemSizes(private val estimate: Float) {
    private val measured = HashMap<Any, Float>()
    private var array = FloatArray(0)

    var version by mutableIntStateOf(0)
        private set

    /** Read [version] first — that is the subscription; this is just the data. */
    val sizes: FloatArray get() = array

    /**
     * Lines the sizes up with a new version of the list. Not a state change in itself: the list
     * changing already recomposes everything that reads these.
     */
    fun lineUp(keys: List<Any>) {
        array = FloatArray(keys.size) { measured[keys[it]] ?: estimate }
        // Measurements of items that have gone are dropped now and then, not on every change.
        if (measured.size > keys.size * 2 + 64) measured.keys.retainAll(keys.toHashSet())
    }

    /**
     * Records a measured size for the item [key], currently at [index]. Ignores non-positive sizes and
     * changes under half a pixel, whose re-layout would otherwise produce another wobble every frame.
     */
    fun measure(key: Any, index: Int, size: Float) {
        if (size <= 0f) return
        val previous = measured[key]
        if (previous != null && abs(previous - size) < 0.5f) return
        measured[key] = size
        if (index in array.indices) array[index] = size
        version++
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
 * <p>Virtualises by row, not by cell: whole rows of [columns] cells mount and unmount together.
 *
 * <p>A trailing partial row is padded with empty cells, so the last row lines up with the ones above
 * rather than centring or stretching.
 *
 * <p>Cells are [cellWidth] by [cellHeight] unless [variableHeight] is set. Then each row is as tall as
 * its tallest cell, every cell in it stretches to match, and [cellHeight] is only the estimate for
 * rows that have not been on screen — the same trade [LazyColumn] makes. Widths stay fixed either
 * way: the columns have to line up.
 *
 * <p>A cell's `rememberSaveable` state follows its [key] while it scrolls out and back. A cell an edit
 * moves into another row is rebuilt there, and keeps neither `remember` nor `rememberSaveable` state:
 * Compose keys saved state by its position in the composition, and each row is a separate position.
 * Keep such state above the grid, keyed by item. When [columns] changes the grid stays on the cells it
 * was showing.
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
    val rows = remember(rowCount) { List(rowCount) { it } }
    val cellKeys = remember(Identity(items), items.size, key != null) { ItemKeys(itemKeys(items, key)) }
    val cellStates = rememberSaveableStateHolder()
    val everMounted = rememberSaveable { HashSet<Any>() }

    DisposableEffect(cellKeys) {
        everMounted.removeAll { stale -> (cellKeys.indexOf(stale) < 0).also { gone -> if (gone) cellStates.removeState(stale) } }
        onDispose { }
    }

    LazyList(
        vertical = true, items = rows, itemSize = cellHeight, variableSize = variableHeight,
        layout = layout, style = style, classes = classes, id = id,
        enabled = enabled, visible = visible, focusable = focusable, events = events,
        overscan = overscan,
        // Keyed by the column count too, so a new count is a new list and the anchor can be remapped.
        key = { _, row -> GridRow(columns, row) },
        remapAnchor = { old, index -> if (old is GridRow) old.row * old.columns / columns else index },
    ) { _, row ->
        // Row aligns to the top by default; a variable row stretches, or a short cell stops short of its neighbours.
        Row(layout = { if (variableHeight) alignItems(AlignItems.STRETCH) else height(cellHeight.px) }) {
            for (column in 0 until columns) {
                val index = row * columns + column
                if (index < items.size) {
                    val item = items[index]
                    val cellKey = cellKeys.keys[index]
                    everMounted.add(cellKey)
                    key(cellKey) {
                        cellStates.SaveableStateProvider(cellKey) {
                            Box(layout = { cell(cellWidth, cellHeight, variableHeight) }) { itemContent(index, item) }
                        }
                    }
                } else {
                    Box(layout = { cell(cellWidth, cellHeight, variableHeight) })
                }
            }
        }
    }
}

private data class GridRow(val columns: Int, val row: Int)

private fun TaffyLayoutStyleDsl.cell(cellWidth: Float, cellHeight: Float, variableHeight: Boolean) {
    width(cellWidth.px)
    if (!variableHeight) height(cellHeight.px)
}

/** An empty element sized by [layout], for gaps. The lazy containers use it for unmounted items. */
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
