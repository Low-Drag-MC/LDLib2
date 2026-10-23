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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
 * being scrolled out of view. `rememberSaveable` state does: each row saves under its [key] on the way
 * out and gets it back on the way in, and that saved state is dropped once its key is no longer in
 * [items]. Shared state — a selection across rows — still belongs above this call.
 *
 * <p>When [items] changes, the list stays on the row it was showing: the first visible row keeps its
 * place on screen, wherever it moved to in the list, instead of the scroll position staying at the
 * same fraction of a list that is now a different length.
 *
 * <p>[items] is compared by identity to notice a change. Hand over a new list, or a snapshot state
 * list whose size changes; replacing one element of a snapshot state list in place is not seen until
 * the next change that is.
 *
 * @param itemHeight with [variableHeight] off, the height every row *is* — nothing enforces it, so
 *   give each row that height (`layout = { height(itemHeight.px) }`) or the spacers and the rows will
 *   disagree. With [variableHeight] on, only the estimate for rows not yet on screen.
 * @param variableHeight lets rows be different heights, at the cost of a wrapper element per row and
 *   a settling frame; see [MeasuredItem].
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

/**
 * The machinery behind [LazyColumn] and [LazyRow], which differ only in the axis.
 *
 * <p>With [variableSize] off the window is plain arithmetic on [itemSize]. With it on, each item is
 * wrapped so it can report its extent, and the window is worked out from what has been measured so
 * far.
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
    require(overscan >= 0) { "overscan must not be negative, was $overscan" }
    val view = remember { ScrollerView() }
    val scroller = if (vertical) view.verticalScroller else view.horizontalScroller
    val metrics = rememberScrollMetrics(view, scroller) {
        if (vertical) view.viewPort.contentHeight else view.viewPort.contentWidth
    }
    // Where an item's rememberSaveable state waits while it is scrolled out.
    val itemStates = rememberSaveableStateHolder()
    // Recomputed when the list changes, not every frame: that is the only O(n) work here.
    val keys = remember(Identity(items), items.size, key != null) { ItemKeys(itemKeys(items, key)) }
    val total = keys.size

    val sizes = if (variableSize) remember(itemSize) { ItemSizes(itemSize) } else null
    if (sizes != null) {
        remember(keys, sizes) { sizes.lineUp(keys.keys) }
        // The subscription. `sizes` is a plain array behind it — see ItemSizes for why.
        @Suppress("UNUSED_EXPRESSION") sizes.version
    }
    val extentOf: (Int) -> Float = if (sizes != null) { i -> sizes.sizes[i] } else { _ -> itemSize }
    val viewport = metrics.viewport.takeIf { it.isFinite() && it > 0f } ?: 0f
    var fraction = metrics.fraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f

    // Stay on the row that was showing when the list changed.
    val anchor = remember { ScrollAnchor() }
    if (anchor.keys !== keys && anchor.keys != null && viewport > 0f) {
        val moved = anchor.key?.let(keys::indexOf) ?: -1
        if (moved >= 0) {
            var start = 0f
            var content = 0f
            for (i in 0 until total) {
                if (i == moved) start = content
                content += extentOf(i)
            }
            val maxScroll = content - viewport
            val target = if (maxScroll > 0f) ((start + anchor.offset) / maxScroll).coerceIn(0f, 1f) else 0f
            if (abs(target - fraction) > 1e-4f) {
                fraction = target
                SideEffect { CompositionWrites.writing { scroller.setValue(target) } }
            }
        }
    }
    anchor.keys = keys

    val window = if (sizes != null) {
        measuredWindow(sizes.sizes, viewport, fraction, overscan)
    } else {
        fixedWindow(total, itemSize, viewport, fraction, overscan)
    }
    if (window.firstVisible >= 0) {
        anchor.key = keys.keys[window.firstVisible]
        anchor.offset = window.firstVisibleOffset
    }

    // Saved state belongs to keys still in the list. Pruned when the list changes, after the
    // composition that dropped them, so nothing still mounted is touched.
    DisposableEffect(keys) {
        val gone = anchor.everMounted.filter { keys.indexOf(it) < 0 }
        for (stale in gone) {
            itemStates.removeState(stale)
            anchor.everMounted.remove(stale)
        }
        onDispose { }
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
        AxisSpacer(vertical, window.leading)
        for (index in window.range) {
            val item = items[index]
            val itemKey = keys.keys[index]
            anchor.everMounted.add(itemKey)
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

/** Which row the list was showing, carried across a change to the list. */
private class ScrollAnchor {
    var keys: ItemKeys? = null
    var key: Any? = null
    var offset = 0f

    /** Every key that has been composed, so their saved state can be dropped once they are gone. */
    val everMounted = HashSet<Any>()
}

/** A key that appeared more than once in a list, told apart by which occurrence it is. */
internal data class RepeatedKey(val key: Any, val occurrence: Int)

/**
 * The key of every item. Index when no [key] lambda is given.
 *
 * <p>A repeated key is made distinct by the order it appears in. Compose itself would cope with the
 * repetition, but the saved-state holder each item is registered with does not: it refuses the second
 * registration, and that stops the whole composition.
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
 * Wraps one variable-size item so it can report how big it turned out to be.
 *
 * <p>An item can only be measured once it has been laid out, which is a frame after it was composed —
 * so the first frame an item appears it is still occupying its estimate, and the window corrects on
 * the next. That settling is visible as a small scrollbar adjustment when you scroll into fresh
 * items, and it is the price of not knowing sizes up front.
 *
 * <p>Fills the cross axis, extent along the scrolling axis left to the content. An item whose content
 * does not size itself measures zero, which [ItemSizes.measure] refuses — otherwise a list of
 * zero-size items would decide the whole list fits on screen and mount all of it.
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
 * Which slice of a list is worth having mounted, how much blank space to leave either side of it, and
 * which item is at the top of the viewport.
 *
 * <p>`leading + (sizes over range) + trailing` always equals the total, which is what keeps the
 * scrollbar honest: the spacers plus the mounted items add up to the whole list.
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
 * <p>Pure, like [measuredWindow], so it can be tested without a UI — `src/test` has no Minecraft on its
 * classpath, and this arithmetic is the part that is easy to get subtly wrong at the ends of the range.
 *
 * @param viewportSize the visible extent along the scrolling axis. Zero before the first layout,
 *   which is why that case has to produce a usable window rather than an empty one.
 */
internal fun fixedWindow(
    total: Int,
    itemSize: Float,
    viewportSize: Float,
    scrollFraction: Float,
    overscan: Int,
): LazyWindow {
    if (total <= 0) return EMPTY_WINDOW
    // Before the first layout the viewport is still zero; mounting one item plus the overscan is
    // enough to get something on screen, and the next frame corrects it.
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
 * <p>Takes the sizes rather than measuring anything, so it stays pure and testable — and because the
 * sizes it is given are a mixture of measured and estimated by nature. An item that has never been on
 * screen has never been laid out, so its size is a guess until it has been; the window this returns is
 * correct for the numbers it was handed, and self-corrects as guesses are replaced.
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

    // First item whose far edge is past the scroll position, tracking its offset as we go.
    var first = 0
    var firstOffset = 0f
    while (first < total - 1 && firstOffset + sizes[first] <= scrolled) {
        firstOffset += sizes[first]
        first++
    }

    // Then forward until the viewport is covered. Zero viewport means the first layout has not
    // happened yet; one item is enough to get something on screen and the next frame corrects it.
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
 * The extents of a list's items along its scrolling axis, measured where they have been on screen
 * and estimated where not.
 *
 * <p>Measurements are kept by item key, not by position, so they move with their items: removing the
 * first row of a list does not hand every other row its predecessor's height.
 *
 * <p>[version] rather than the array itself is the observable state. An item reports its size from a
 * layout event, and copying a ten-thousand-entry array on each of those — several per frame while
 * scrolling — costs more than the virtualisation saves. So the array is mutated in place and the
 * counter is bumped, which is what readers actually subscribe to.
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
     * Records a measured size for the item [key], currently at [index].
     *
     * <p>Ignores changes under half a pixel. Layout produces fractional values, and an item whose
     * size wobbles in the noise would invalidate the window every frame, which re-lays-out the items,
     * which produces another wobble.
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
 *
 * <p>A cell's `rememberSaveable` state follows its [key] even when an insertion moves it to another
 * row; plain `remember` state does not, since the cell is rebuilt in its new row.
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
    // Keyed on the count, not the list: a list mutated in place is the same instance, and comparing
    // a list by equality walks every element on every recomposition.
    val rows = remember(rowCount) { List(rowCount) { it } }
    val cellKeys = remember(Identity(items), items.size, key != null) { itemKeys(items, key) }
    val cellStates = rememberSaveableStateHolder()

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
                    val cellKey = cellKeys[index]
                    key(cellKey) {
                        cellStates.SaveableStateProvider(cellKey) {
                            Box(layout = { cell(cellWidth, cellHeight, variableHeight) }) {
                                itemContent(index, item)
                            }
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
 * An empty element, sized by [layout].
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
