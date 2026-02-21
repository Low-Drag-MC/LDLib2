package com.lowdragmc.lowdraglib2.gui.ui.layout

import com.lowdragmc.lowdraglib2.gui.ui.UIDslMarker
import com.lowdragmc.lowdraglib2.gui.ui.data.Grid
import com.lowdragmc.lowdraglib2.gui.ui.data.GridAuto
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplateAreas
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle
import dev.vfyjxf.taffy.style.AlignContent
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import dev.vfyjxf.taffy.style.FlexWrap
import dev.vfyjxf.taffy.style.GridAutoFlow
import dev.vfyjxf.taffy.style.TaffyDirection
import dev.vfyjxf.taffy.style.TaffyDisplay
import dev.vfyjxf.taffy.style.TaffyPosition

sealed interface LP {
    data class Px(val value: Float) : LP
    data class Percent(val value: Float) : LP // 0..100
    data object Auto : LP
}

val Number.px: LP get() = LP.Px(this.toFloat())
val Number.pct: LP get() = LP.Percent(this.toFloat())
val auto: LP get() = LP.Auto

@UIDslMarker
class TaffyLayoutStyleDsl(val layout: LayoutStyle) {
    private fun apply(action: (LayoutStyle) -> Unit) {
        action(layout)
    }

    /**
     * Escape hatch: directly mutate LayoutStyle.
     */
    fun raw(block: LayoutStyle.() -> Unit) {
        apply { it.block() }
    }

    // ----------------------------
    // size
    // ----------------------------
    fun width(px: Number) = apply { it.width(px.toFloat()) }
    fun width(value: LP) = apply {
        when (value) {
            is LP.Px -> it.width(value.value)
            is LP.Percent -> it.widthPercent(value.value)
            LP.Auto -> it.widthAuto()
        }
    }

    fun height(px: Number) = apply { it.height(px.toFloat()) }
    fun height(value: LP) = apply {
        when (value) {
            is LP.Px -> it.height(value.value)
            is LP.Percent -> it.heightPercent(value.value)
            LP.Auto -> it.heightAuto()
        }
    }

    fun size(width: LP, height: LP) {
        width(width)
        height(height)
    }

    fun size(size: LP) {
        width(size)
        height(size)
    }

    fun minWidth(px: Number) = apply { it.minWidth(px.toFloat()) }
    fun minWidth(value: LP) = apply {
        when (value) {
            is LP.Px -> it.minWidth(value.value)
            is LP.Percent -> it.minWidthPercent(value.value)
            LP.Auto -> it.minWidthAuto()
        }
    }

    fun minHeight(px: Number) = apply { it.minHeight(px.toFloat()) }
    fun minHeight(value: LP) = apply {
        when (value) {
            is LP.Px -> it.minHeight(value.value)
            is LP.Percent -> it.minHeightPercent(value.value)
            LP.Auto -> it.minHeightAuto()
        }
    }

    fun minSize(width: LP, height: LP) {
        minWidth(width)
        minHeight(height)
    }

    fun minSize(size: LP) {
        minWidth(size)
        minHeight(size)
    }

    fun maxWidth(px: Number) = apply { it.maxWidth(px.toFloat()) }
    fun maxWidth(value: LP) = apply {
        when (value) {
            is LP.Px -> it.maxWidth(value.value)
            is LP.Percent -> it.maxWidthPercent(value.value)
            LP.Auto -> it.maxWidthAuto()
        }
    }

    fun maxHeight(px: Number) = apply { it.maxHeight(px.toFloat()) }
    fun maxHeight(value: LP) = apply {
        when (value) {
            is LP.Px -> it.maxHeight(value.value)
            is LP.Percent -> it.maxHeightPercent(value.value)
            LP.Auto -> it.maxHeightAuto()
        }
    }

    fun maxSize(width: LP, height: LP) {
        maxWidth(width)
        maxHeight(height)
    }

    fun maxSize(size: LP) {
        maxWidth(size)
        maxHeight(size)
    }

    fun aspectRatio(value: Number) = apply { it.aspectRatio(value.toFloat()) }
    fun aspectRatioAuto() = apply { it.aspectRatioAuto() }

    // ----------------------------
    // margin / padding / gap / position scopes
    // ----------------------------

    @UIDslMarker
    class BoxScope internal constructor(
        private val add: ((LayoutStyle) -> Unit) -> Unit,
        private val kind: Kind
    ) {
        internal enum class Kind { MARGIN, PADDING }

        fun left(v: Number) = left(v.px)
        fun top(v: Number) = top(v.px)
        fun right(v: Number) = right(v.px)
        fun bottom(v: Number) = bottom(v.px)
        fun horizontal(v: Number) = horizontal(v.px)
        fun vertical(v: Number) = vertical(v.px)
        fun all(v: Number) = all(v.px)

        fun left(v: LP) = add {
            when (v) {
                is LP.Px -> if (kind == Kind.MARGIN) it.marginLeft(v.value) else it.paddingLeft(v.value)
                is LP.Percent -> if (kind == Kind.MARGIN) it.marginLeftPercent(v.value) else it.paddingLeftPercent(v.value)
                LP.Auto -> if (kind == Kind.MARGIN) it.marginLeftAuto() else it.paddingLeft(0f)
            }
        }

        fun top(v: LP) = add {
            when (v) {
                is LP.Px -> if (kind == Kind.MARGIN) it.marginTop(v.value) else it.paddingTop(v.value)
                is LP.Percent -> if (kind == Kind.MARGIN) it.marginTopPercent(v.value) else it.paddingTopPercent(v.value)
                LP.Auto -> if (kind == Kind.MARGIN) it.marginTopAuto() else it.paddingTop(0f)
            }
        }

        fun right(v: LP) = add {
            when (v) {
                is LP.Px -> if (kind == Kind.MARGIN) it.marginRight(v.value) else it.paddingRight(v.value)
                is LP.Percent -> if (kind == Kind.MARGIN) it.marginRightPercent(v.value) else it.paddingRightPercent(v.value)
                LP.Auto -> if (kind == Kind.MARGIN) it.marginRightAuto() else it.paddingRight(0f)
            }
        }

        fun bottom(v: LP) = add {
            when (v) {
                is LP.Px -> if (kind == Kind.MARGIN) it.marginBottom(v.value) else it.paddingBottom(v.value)
                is LP.Percent -> if (kind == Kind.MARGIN) it.marginBottomPercent(v.value) else it.paddingBottomPercent(v.value)
                LP.Auto -> if (kind == Kind.MARGIN) it.marginBottomAuto() else it.paddingBottom(0f)
            }
        }

        fun horizontal(v: LP) = add {
            when (v) {
                is LP.Px -> if (kind == Kind.MARGIN) it.marginHorizontal(v.value) else it.paddingHorizontal(v.value)
                is LP.Percent -> if (kind == Kind.MARGIN) it.marginHorizontalPercent(v.value) else it.paddingHorizontalPercent(v.value)
                LP.Auto -> if (kind == Kind.MARGIN) it.marginHorizontalAuto() else it.paddingHorizontal(0f)
            }
        }

        fun vertical(v: LP) = add {
            when (v) {
                is LP.Px -> if (kind == Kind.MARGIN) it.marginVertical(v.value) else it.paddingVertical(v.value)
                is LP.Percent -> if (kind == Kind.MARGIN) it.marginVerticalPercent(v.value) else it.paddingVerticalPercent(v.value)
                LP.Auto -> if (kind == Kind.MARGIN) it.marginVerticalAuto() else it.paddingVertical(0f)
            }
        }

        fun all(v: LP) = add {
            when (v) {
                is LP.Px -> if (kind == Kind.MARGIN) it.marginAll(v.value) else it.paddingAll(v.value)
                is LP.Percent -> if (kind == Kind.MARGIN) it.marginAllPercent(v.value) else it.paddingAllPercent(v.value)
                LP.Auto -> if (kind == Kind.MARGIN) it.marginAllAuto() else it.paddingAll(0f)
            }
        }

    }

    fun margin(block: BoxScope.() -> Unit) {
        BoxScope(::apply, BoxScope.Kind.MARGIN).apply(block)
    }

    fun padding(block: BoxScope.() -> Unit) {
        BoxScope(::apply, BoxScope.Kind.PADDING).apply(block)
    }

    @UIDslMarker
    class GapScope internal constructor(private val add: ((LayoutStyle) -> Unit) -> Unit) {
        fun row(value: Number) = row(value.px)
        fun column(value: Number) = column(value.px)
        fun all(value: Number) = all(value.px)

        fun row(v: LP) = add {
            when (v) {
                is LP.Px -> it.gapRow(v.value)
                is LP.Percent -> it.gapRowPercent(v.value)
                LP.Auto -> it.gapRow(0f)
            }
        }

        fun column(v: LP) = add {
            when (v) {
                is LP.Px -> it.gapColumn(v.value)
                is LP.Percent -> it.gapColumnPercent(v.value)
                LP.Auto -> it.gapColumn(0f)
            }
        }

        fun all(v: LP) = add {
            when (v) {
                is LP.Px -> it.gapAll(v.value)
                is LP.Percent -> it.gapAllPercent(v.value)
                LP.Auto -> it.gapAll(0f)
            }
        }

    }

    fun gap(block: GapScope.() -> Unit) {
        GapScope(::apply).apply(block)
    }

    @UIDslMarker
    class PositionScope internal constructor(private val add: ((LayoutStyle) -> Unit) -> Unit) {
        fun type(value: TaffyPosition) = add { it.positionType(value) }

        fun left(v: Number) = left(v.px)
        fun top(v: Number) = top(v.px)
        fun right(v: Number) = right(v.px)
        fun bottom(v: Number) = bottom(v.px)

        fun left(v: LP) = add {
            when (v) {
                is LP.Px -> it.left(v.value)
                is LP.Percent -> it.leftPercent(v.value)
                LP.Auto -> it.leftAuto()
            }
        }

        fun top(v: LP) = add {
            when (v) {
                is LP.Px -> it.top(v.value)
                is LP.Percent -> it.topPercent(v.value)
                LP.Auto -> it.topAuto()
            }
        }

        fun right(v: LP) = add {
            when (v) {
                is LP.Px -> it.right(v.value)
                is LP.Percent -> it.rightPercent(v.value)
                LP.Auto -> it.rightAuto()
            }
        }

        fun bottom(v: LP) = add {
            when (v) {
                is LP.Px -> it.bottom(v.value)
                is LP.Percent -> it.bottomPercent(v.value)
                LP.Auto -> it.bottomAuto()
            }
        }
    }

    fun pos(block: PositionScope.() -> Unit) {
        PositionScope(::apply).apply(block)
    }

    // ----------------------------
    // flex / alignment / display / overflow / direction
    // ----------------------------

    fun flex(value: Number) = apply { it.flex(value.toFloat()) }
    fun flexAuto() = apply { it.flexAuto() }

    fun flexGrow(value: Number) = apply { it.flexGrow(value.toFloat()) }
    fun flexGrowAuto() = apply { it.flexGrowAuto() }

    fun flexShrink(value: Number) = apply { it.flexShrink(value.toFloat()) }
    fun flexShrinkAuto() = apply { it.flexShrinkAuto() }

    fun flexBasis(px: Number) = apply { it.flexBasis(px.toFloat()) }
    fun flexBasis(value: LP) = apply {
        when (value) {
            is LP.Px -> it.flexBasis(value.value)
            is LP.Percent -> it.flexBasisPercent(value.value)
            LP.Auto -> it.flexBasisAuto()
        }
    }

    fun flexDirection(value: FlexDirection) = apply { it.flexDirection(value) }
    fun wrap(value: FlexWrap) = apply { it.wrap(value) }

    fun justifyContent(value: AlignContent) = apply { it.justifyContent(value) }
    fun justifyItems(value: AlignItems) = apply { it.justifyItems(value) }
    fun justifySelf(value: AlignItems) = apply { it.justifySelf(value) }

    fun alignItems(value: AlignItems) = apply { it.alignItems(value) }
    fun alignSelf(value: AlignItems) = apply { it.alignSelf(value) }
    fun alignContent(value: AlignContent) = apply { it.alignContent(value) }

    fun display(value: Boolean) = apply { it.display(if (value) TaffyDisplay.FLEX else TaffyDisplay.NONE) }
    fun display(value: TaffyDisplay) = apply { it.display(value) }
    fun direction(value: TaffyDirection) = apply { it.direction(value) }
    fun position(value: TaffyPosition) = apply { it.positionType(value) }
//    fun overflow(value: YogaOverflow) = add { it.overflow(value) }

    // ----------------------------
    // grid
    // ----------------------------

    @UIDslMarker
    class GridScope internal constructor(private val add: ((LayoutStyle) -> Unit) -> Unit) {
        fun templateRows(value: String) = add { it.gridTemplateRows(value) }
        fun templateRows(value: GridTemplate) = add { it.gridTemplateRows(value) }

        fun templateColumns(value: String) = add { it.gridTemplateColumns(value) }
        fun templateColumns(value: GridTemplate) = add { it.gridTemplateColumns(value) }

        fun templateAreas(value: String) = add { it.gridTemplateAreas(value) }
        fun templateAreas(value: GridTemplateAreas) = add { it.gridTemplateAreas(value) }

        fun autoRows(value: String) = add { it.gridAutoRows(value) }
        fun autoRows(value: GridAuto) = add { it.gridAutoRows(value) }

        fun autoColumns(value: String) = add { it.gridAutoColumns(value) }
        fun autoColumns(value: GridAuto) = add { it.gridAutoColumns(value) }

        fun autoFlow(value: GridAutoFlow) = add { it.gridAutoFlow(value) }

        fun row(value: String) = add { it.gridRow(value) }
        fun row(value: Grid) = add { it.gridRow(value) }

        fun column(value: String) = add { it.gridColumn(value) }
        fun column(value: Grid) = add { it.gridColumn(value) }
    }

    fun grid(block: GridScope.() -> Unit) {
        GridScope(::apply).apply(block)
    }
}