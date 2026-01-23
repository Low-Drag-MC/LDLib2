package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridAuto;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplateAreas;
import com.lowdragmc.lowdraglib2.gui.ui.data.LPARect;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.*;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TaffyLayoutStyle {
    public static final TaffyStyle DEFAULT_TAFFY_STYLE = new TaffyStyle();
    static {
        DEFAULT_TAFFY_STYLE.flexDirection = FlexDirection.COLUMN;
        DEFAULT_TAFFY_STYLE.flexShrink = 0;
        DEFAULT_TAFFY_STYLE.minSize = TaffySize.all(TaffyDimension.ZERO);
        DEFAULT_TAFFY_STYLE.alignContent = AlignContent.FLEX_START;
    }

    public final UIElement element;
    public final TaffyStyle style;
    // runtime
    public final LPARectData margin;
    public final LPRectData padding;


    public TaffyLayoutStyle(UIElement element) {
        this.element = element;
        this.style = DEFAULT_TAFFY_STYLE.copy();
        this.margin = new LPARectData(() -> style.margin, margin -> {
            style.margin = margin;
            element.markTaffyStyleDirty();
        });
        this.padding = new LPRectData(() -> style.padding, padding -> {
            style.padding = padding;
            element.markTaffyStyleDirty();
        });
    }

    public static TaffyDimension parseDimension(StyleSizeLength value) {
        TaffyDimension dimension;
        if (value.isAuto()) {
            dimension = TaffyDimension.AUTO;
        } else if (value.isPercent()) {
            dimension = TaffyDimension.percent(value.value().getValue() / 100f);
        } else if (value.isPoints()) {
            dimension = TaffyDimension.length(value.value().getValue());
        } else {
            dimension = TaffyDimension.AUTO;
        }
        return dimension;
    }

    public static LengthPercentageAuto parseLengthPercentageAuto(StyleLength styleLength) {
        if (styleLength.isAuto()) {
            return LengthPercentageAuto.AUTO;
        } else if (styleLength.isPercent()) {
            return LengthPercentageAuto.percent(styleLength.value().getValue() / 100f);
        } else if (styleLength.isPoints()) {
            return LengthPercentageAuto.length(styleLength.value().getValue());
        }
        return LengthPercentageAuto.AUTO;
    }

    public static LengthPercentage parseLengthPercentage(StyleLength styleLength) {
        if (styleLength.isPercent()) {
            return LengthPercentage.percent(styleLength.value().getValue() / 100f);
        } else if (styleLength.isPoints()) {
            return LengthPercentage.length(styleLength.value().getValue());
        }
        return LengthPercentage.ZERO;
    }

    public void setDisplay(TaffyDisplay display) {
        if (style.display != display) {
            style.display = display;
            element.markTaffyStyleDirty();
        }
    }

    public void setDirection(TaffyDirection direction) {
        if (style.direction != direction) {
            style.direction = direction;
            element.markTaffyStyleDirty();
        }
    }

    public void setFlexBasis(StyleSizeLength value) {
        var flexBasis = parseDimension(value);
        if (!style.flexBasis.equals(flexBasis)) {
            style.flexBasis = flexBasis;
            element.markTaffyStyleDirty();
        }
    }

    public void setFlex(FloatOptional value) {
        var flex = value.isUndefined() ? Float.NaN : value.getValue();
        if (style.flex != flex) {
            style.flex = flex;
            element.markTaffyStyleDirty();
        }
    }

    public void setFlexGrow(FloatOptional value) {
        var flexGrow = value.isUndefined() ? 0 : value.getValue();
        if (style.flexGrow != flexGrow) {
            style.flexGrow = flexGrow;
            element.markTaffyStyleDirty();
        }
    }

    public void setFlexShrink(FloatOptional value) {
        var flexShrink = value.isUndefined() ? 0 : value.getValue();
        if (style.flexShrink != flexShrink) {
            style.flexShrink = flexShrink;
            element.markTaffyStyleDirty();
        }
    }

    private static AlignItems parseAlignItems(YogaAlign value) {
        return switch (value) {
            case AUTO, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY -> null;
            case FLEX_START -> AlignItems.FLEX_START;
            case CENTER -> AlignItems.CENTER;
            case FLEX_END -> AlignItems.FLEX_END;
            case STRETCH -> AlignItems.STRETCH;
            case BASELINE -> AlignItems.BASELINE;
        };
    }

    public void setFlexDirection(FlexDirection flexDirection) {
        if (style.flexDirection != flexDirection) {
            style.flexDirection = flexDirection;
            element.markTaffyStyleDirty();
        }
    }

    public void setFlexWrap(FlexWrap flexWrap) {
        if (style.flexWrap != flexWrap) {
            style.flexWrap = flexWrap;
            element.markTaffyStyleDirty();
        }
    }

    public void setPosition(TaffyPosition position) {
        if (style.position != position) {
            style.position = position;
            element.markTaffyStyleDirty();
        }
    }

    public void setOverFlow(YogaOverflow value) {
        TaffyPoint<Overflow> overflow = switch (value) {
            case VISIBLE -> new TaffyPoint<>(Overflow.VISIBLE, Overflow.VISIBLE);
            case HIDDEN -> new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
            case SCROLL -> new TaffyPoint<>(Overflow.SCROLL, Overflow.SCROLL);
        };
        if (!style.overflow.equals(overflow)) {
            style.overflow = overflow;
            element.markTaffyStyleDirty();
        }
    }

    public void setAlignItems(AlignItems alignItems) {
        if (style.alignItems != alignItems) {
            style.alignItems = alignItems;
            element.markTaffyStyleDirty();
        }
    }

    public void setJustifyContent(AlignContent justifyContent) {
        if (style.justifyContent != justifyContent) {
            style.justifyContent = justifyContent;
            element.markTaffyStyleDirty();
        }
    }

    public void setJustifySelf(AlignItems justifySelf) {
        if (style.justifySelf != justifySelf) {
            style.justifySelf = justifySelf;
            element.markTaffyStyleDirty();
        }
    }

    public void setJustifyItems(AlignItems justifyItems) {
        if (style.justifyItems != justifyItems) {
            style.justifyItems = justifyItems;
            element.markTaffyStyleDirty();
        }
    }

    public void setAlignSelf(AlignItems alignSelf) {
        if (style.alignSelf != alignSelf) {
            style.alignSelf = alignSelf;
            element.markTaffyStyleDirty();
        }
    }

    public void setAlignContent(AlignContent alignContent) {
        if (style.alignContent != alignContent) {
            style.alignContent = alignContent;
            element.markTaffyStyleDirty();
        }
    }

    public void setAspectRate(FloatOptional value) {
        Float aspectRatio = value.isUndefined() ? DEFAULT_TAFFY_STYLE.aspectRatio : (Float) value.getValue();
        if (!Objects.equals(style.aspectRatio, aspectRatio)) {
            style.aspectRatio = aspectRatio;
            element.markTaffyStyleDirty();
        }
    }

    public void setWidth(StyleSizeLength value) {
        var width = parseDimension(value);
        if (!Objects.equals(style.size.width, width)) {
            style.size = new TaffySize<>(width, style.size.height);
            element.markTaffyStyleDirty();
        }
    }

    public void setHeight(StyleSizeLength value) {
        var height = parseDimension(value);
        if (!Objects.equals(style.size.height, height)) {
            style.size = new TaffySize<>(style.size.width, height);
            element.markTaffyStyleDirty();
        }
    }

    public void setInset(YogaEdge edge, StyleLength value) {
        var length = parseLengthPercentageAuto(value);
        TaffyRect<LengthPercentageAuto> rect = switch (edge) {
            case LEFT -> TaffyRect.of(length, style.inset.right, style.inset.top, style.inset.bottom);
            case TOP -> TaffyRect.of(style.inset.left, style.inset.right, length, style.inset.bottom);
            case RIGHT -> TaffyRect.of(style.inset.left, length, style.inset.top, style.inset.bottom);
            case BOTTOM -> TaffyRect.of(style.inset.left, style.inset.right, style.inset.top, length);
            case START -> TaffyRect.of(length, style.inset.right, length, style.inset.bottom);
            case END -> TaffyRect.of(style.inset.left, length, style.inset.top, length);
            case VERTICAL -> TaffyRect.of(style.inset.left, style.inset.right, length, length);
            case HORIZONTAL -> TaffyRect.of(length, length, style.inset.top, style.inset.bottom);
            case ALL -> TaffyRect.all(length);
        };
        if (!Objects.equals(style.inset, rect)) {
            style.inset = rect;
            element.markTaffyStyleDirty();
        }
    }

    public void setMargin(YogaEdge edge, StyleLength value) {
        var length = parseLengthPercentageAuto(value);
        TaffyRect<LengthPercentageAuto> rect = switch (edge) {
            case LEFT -> TaffyRect.of(length, style.margin.right, style.margin.top, style.margin.bottom);
            case TOP -> TaffyRect.of(style.margin.left, style.margin.right, length, style.margin.bottom);
            case RIGHT -> TaffyRect.of(style.margin.left, length, style.margin.top, style.margin.bottom);
            case BOTTOM -> TaffyRect.of(style.margin.left, style.margin.right, style.margin.top, length);
            case START -> TaffyRect.of(length, style.margin.right, length, style.margin.bottom);
            case END -> TaffyRect.of(style.margin.left, length, style.margin.top, length);
            case VERTICAL -> TaffyRect.of(style.margin.left, style.margin.right, length, length);
            case HORIZONTAL -> TaffyRect.of(length, length, style.margin.top, style.margin.bottom);
            case ALL -> TaffyRect.all(length);
        };
        if (!Objects.equals(style.margin, rect)) {
            style.margin = rect;
            element.markTaffyStyleDirty();
        }
    }

    public void setPadding(YogaEdge edge, StyleLength value) {
        var length = parseLengthPercentage(value);
        TaffyRect<LengthPercentage> rect = switch (edge) {
            case LEFT -> TaffyRect.of(length, style.padding.right, style.padding.top, style.padding.bottom);
            case TOP -> TaffyRect.of(style.padding.left, style.padding.right, length, style.padding.bottom);
            case RIGHT -> TaffyRect.of(style.padding.left, length, style.padding.top, style.padding.bottom);
            case BOTTOM -> TaffyRect.of(style.padding.left, style.padding.right, style.padding.top, length);
            case START -> TaffyRect.of(length, style.padding.right, length, style.padding.bottom);
            case END -> TaffyRect.of(style.padding.left, length, style.padding.top, length);
            case VERTICAL -> TaffyRect.of(style.padding.left, style.padding.right, length, length);
            case HORIZONTAL -> TaffyRect.of(length, length, style.padding.top, style.padding.bottom);
            case ALL -> TaffyRect.all(length);
        };
        if (!Objects.equals(style.padding, rect)) {
            style.padding = rect;
            element.markTaffyStyleDirty();
        }
    }

    public void setGap(YogaGutter gutter, StyleLength value) {
        var length = parseLengthPercentage(value);
        TaffySize<LengthPercentage> size = switch (gutter) {
            case COLUMN -> new TaffySize<>(length, style.gap.height);
            case ROW -> new TaffySize<>(style.gap.width, length);
            case ALL -> new TaffySize<>(length, length);
        };
        if (!Objects.equals(style.gap, size)) {
            style.gap = size;
            element.markTaffyStyleDirty();
        }
    }

    public void setMinSize(YogaDimension dimension, StyleSizeLength value) {
        var dim = parseDimension(value);
        TaffySize<TaffyDimension> size = switch (dimension) {
            case WIDTH -> new TaffySize<>(dim, style.minSize.height);
            case HEIGHT -> new TaffySize<>(style.minSize.width, dim);
        };
        if (!Objects.equals(style.minSize, size)) {
            style.minSize = size;
            element.markTaffyStyleDirty();
        }
    }

    public void setMaxSize(YogaDimension dimension, StyleSizeLength value) {
        var dim = parseDimension(value);
        TaffySize<TaffyDimension> size = switch (dimension) {
            case WIDTH -> new TaffySize<>(dim, style.maxSize.height);
            case HEIGHT -> new TaffySize<>(style.maxSize.width, dim);
        };
        if (!Objects.equals(style.maxSize, size)) {
            style.maxSize = size;
            element.markTaffyStyleDirty();
        }
    }

    // ==================== Grid Properties ====================

    public void setGridTemplateRows(GridTemplate value) {
        var dirty = false;
        if (!Objects.equals(style.gridTemplateRows, value.simples())) {
            style.gridTemplateRows = value.simples();
            dirty = true;
        }
        if (!Objects.equals(style.gridTemplateRowsWithRepeat, value.repeats())) {
            style.gridTemplateRowsWithRepeat = value.repeats();
            dirty = true;
        }
        if (!Objects.equals(style.gridTemplateRowNames, value.names())) {
            style.gridTemplateRowNames = value.names();
            dirty = true;
        }
        if (dirty) {
            element.markTaffyStyleDirty();
        }
    }

    public void setGridTemplateColumns(GridTemplate value) {
        var dirty = false;
        if (!Objects.equals(style.gridTemplateColumns, value.simples())) {
            style.gridTemplateColumns = value.simples();
            dirty = true;
        }
        if (!Objects.equals(style.gridTemplateColumnsWithRepeat, value.repeats())) {
            style.gridTemplateColumnsWithRepeat = value.repeats();
            dirty = true;
        }
        if (!Objects.equals(style.gridTemplateColumnNames, value.names())) {
            style.gridTemplateColumnNames = value.names();
            dirty = true;
        }
        if (dirty) {
            element.markTaffyStyleDirty();
        }
    }

    public void setGridTemplateAreas(GridTemplateAreas value) {
        if (!Objects.equals(style.gridTemplateAreas, value.areas())) {
            style.gridTemplateAreas = value.areas();
            element.markTaffyStyleDirty();
        }
    }

    public void setGridAutoRows(GridAuto value) {
        if (!Objects.equals(style.gridAutoRows, value.values())) {
            style.gridAutoRows = value.values();
            element.markTaffyStyleDirty();
        }
    }

    public void setGridAutoColumns(GridAuto value) {
        if (!Objects.equals(style.gridAutoColumns, value.values())) {
            style.gridAutoColumns = value.values();
            element.markTaffyStyleDirty();
        }
    }

    public void setGridAutoFlow(GridAutoFlow value) {
        if (style.gridAutoFlow != value) {
            style.gridAutoFlow = value;
            element.markTaffyStyleDirty();
        }
    }

    public void setGridRow(com.lowdragmc.lowdraglib2.gui.ui.data.Grid value) {
        if (!Objects.equals(style.gridRow, value.grid())) {
            style.gridRow = value.grid();
            element.markTaffyStyleDirty();
        }
    }

    public void setGridColumn(com.lowdragmc.lowdraglib2.gui.ui.data.Grid value) {
        if (!Objects.equals(style.gridColumn, value.grid())) {
            style.gridColumn = value.grid();
            element.markTaffyStyleDirty();
        }
    }

    public void setLeft(LengthPercentageAuto left) {
        if (!Objects.equals(style.inset.left, left)) {
            style.inset = new TaffyRect<>(left, style.inset.right, style.inset.top, style.inset.bottom);
            element.markTaffyStyleDirty();
        }
    }

    public void setTop(LengthPercentageAuto top) {
        if (!Objects.equals(style.inset.top, top)) {
            style.inset = new TaffyRect<>(style.inset.left, style.inset.right, top, style.inset.bottom);
        }
    }

    public void setRight(LengthPercentageAuto right) {
        if (!Objects.equals(style.inset.right, right)) {
            style.inset = new TaffyRect<>(style.inset.left, right, style.inset.top, style.inset.bottom);
        }
    }

    public void setBottom(LengthPercentageAuto bottom) {
        if (!Objects.equals(style.inset.bottom, bottom)) {
            style.inset = new TaffyRect<>(style.inset.left, style.inset.right, style.inset.top, bottom);
        }
    }

    public class LPARectData {
        private LengthPercentageAuto left = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto top = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto right = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto bottom = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto vertical = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto horizontal = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto all = LengthPercentageAuto.AUTO;
        private LPARect rect = LPARect.ZERO;

        private final Supplier<TaffyRect<LengthPercentageAuto>> getter;
        private final Consumer<TaffyRect<LengthPercentageAuto>> setter;

        public LPARectData(Supplier<TaffyRect<LengthPercentageAuto>> getter, Consumer<TaffyRect<LengthPercentageAuto>> setter) {
            this.setter = setter;
            this.getter = getter;
        }

        public void setLeft(LengthPercentageAuto left) {
            if (!Objects.equals(this.left, left)) {
                this.left = left;
                onChanged();
            }
        }

        public void setTop(LengthPercentageAuto top) {
            if (!Objects.equals(this.top, top)) {
                this.top = top;
                onChanged();
            }
        }

        public void setRight(LengthPercentageAuto right) {
            if (!Objects.equals(this.right, right)) {
                this.right = right;
                onChanged();
            }
        }

        public void setBottom(LengthPercentageAuto bottom) {
            if (!Objects.equals(this.bottom, bottom)) {
                this.bottom = bottom;
                onChanged();
            }
        }

        public void setVertical(LengthPercentageAuto bottom) {
            if (!Objects.equals(this.vertical, bottom)) {
                this.vertical = bottom;
                onChanged();
            }
        }

        public void setHorizontal(LengthPercentageAuto bottom) {
            if (!Objects.equals(this.horizontal, bottom)) {
                this.horizontal = bottom;
                onChanged();
            }
        }

        public void setAll(LengthPercentageAuto bottom) {
            if (!Objects.equals(this.all, bottom)) {
                this.all = bottom;
                onChanged();
            }
        }

        public void setRect(LPARect rect) {
            if (Objects.equals(this.rect, rect)) {
                this.rect = rect;
                onChanged();
            }
        }

        public void onChanged() {
            var current = getter.get();
            var left = this.left.isAuto() ?
                    (this.horizontal.isAuto() ?
                            (this.all.isAuto() ? this.rect.rect().left :
                                    this.all) :
                            this.horizontal) :
                    this.left;
            var top = this.top.isAuto() ?
                    (this.vertical.isAuto() ?
                            (this.all.isAuto() ? this.rect.rect().top :
                                    this.all) :
                            this.vertical) :
                    this.top;
            var right = this.right.isAuto() ?
                    (this.horizontal.isAuto() ?
                            (this.all.isAuto() ? this.rect.rect().right :
                                    this.all) :
                            this.horizontal) :
                    this.right;
            var bottom = this.bottom.isAuto() ?
                    (this.vertical.isAuto() ?
                            (this.all.isAuto() ? this.rect.rect().bottom :
                                    this.all) :
                            this.vertical) :
                    this.bottom;
            if (!current.left.equals(left) ||
                    !current.top.equals(top) ||
                    !current.right.equals(right) ||
                    !current.bottom.equals(bottom)) {
                setter.accept(TaffyRect.of(left, right, top, bottom));
            }
        }
    }

    public class LPRectData {
        private LengthPercentageAuto left = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto top = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto right = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto bottom = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto vertical = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto horizontal = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto all = LengthPercentageAuto.AUTO;
        private LPARect rect = LPARect.ZERO;

        private final Supplier<TaffyRect<LengthPercentage>> getter;
        private final Consumer<TaffyRect<LengthPercentage>> setter;

        public LPRectData(Supplier<TaffyRect<LengthPercentage>> getter, Consumer<TaffyRect<LengthPercentage>> setter) {
            this.setter = setter;
            this.getter = getter;
        }

        public void setLeft(LengthPercentageAuto left) {
            if (!Objects.equals(this.left, left)) {
                this.left = left;
                onChanged();
            }
        }

        public void setTop(LengthPercentageAuto top) {
            if (!Objects.equals(this.top, top)) {
                this.top = top;
                onChanged();
            }
        }

        public void setRight(LengthPercentageAuto right) {
            if (!Objects.equals(this.right, right)) {
                this.right = right;
                onChanged();
            }
        }

        public void setBottom(LengthPercentageAuto bottom) {
            if (!Objects.equals(this.bottom, bottom)) {
                this.bottom = bottom;
                onChanged();
            }
        }

        public void setVertical(LengthPercentageAuto bottom) {
            if (!Objects.equals(this.vertical, bottom)) {
                this.vertical = bottom;
                onChanged();
            }
        }

        public void setHorizontal(LengthPercentageAuto bottom) {
            if (!Objects.equals(this.horizontal, bottom)) {
                this.horizontal = bottom;
                onChanged();
            }
        }

        public void setAll(LengthPercentageAuto bottom) {
            if (!Objects.equals(this.all, bottom)) {
                this.all = bottom;
                onChanged();
            }
        }

        public void setRect(LPARect rect) {
            if (Objects.equals(this.rect, rect)) {
                this.rect = rect;
                onChanged();
            }
        }

        public void onChanged() {
            var current = getter.get();
            var left = this.left.isAuto() ?
                    (this.horizontal.isAuto() ?
                            (this.all.isAuto() ? this.rect.rect().left :
                                    this.all) :
                            this.horizontal) :
                    this.left;
            var top = this.top.isAuto() ?
                    (this.vertical.isAuto() ?
                            (this.all.isAuto() ? this.rect.rect().top :
                                    this.all) :
                            this.vertical) :
                    this.top;
            var right = this.right.isAuto() ?
                    (this.horizontal.isAuto() ?
                            (this.all.isAuto() ? this.rect.rect().right :
                                    this.all) :
                            this.horizontal) :
                    this.right;
            var bottom = this.bottom.isAuto() ?
                    (this.vertical.isAuto() ?
                            (this.all.isAuto() ? this.rect.rect().bottom :
                                    this.all) :
                            this.vertical) :
                    this.bottom;
            if (!lpaEquals(left, current.left) ||
                    !lpaEquals(top, current.top) ||
                    !lpaEquals(right, current.right) ||
                    !lpaEquals(bottom, current.bottom)) {
                setter.accept(TaffyRect.of(toLP(left), toLP(right), toLP(top), toLP(bottom)));
            }
        }

        public static boolean lpaEquals(LengthPercentageAuto lpa, LengthPercentage lp) {
            if (lpa.isLength() && lp.isLength()) return lpa.getValue() == lp.getValue();
            if (lpa.isPercent() && lp.isPercent()) return lpa.getValue() == lp.getValue();
            return false;
        }

        public static LengthPercentage toLP(LengthPercentageAuto lpa) {
            if (lpa.isPercent()) return LengthPercentage.percent(lpa.getValue());
            if (lpa.isLength()) return LengthPercentage.length(lpa.getValue());
            return LengthPercentage.ZERO;
        }
    }
}