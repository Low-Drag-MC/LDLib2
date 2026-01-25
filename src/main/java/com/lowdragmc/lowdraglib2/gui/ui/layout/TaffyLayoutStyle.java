package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.*;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
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
    public final LPSizeData gap;


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
        this.gap = new LPSizeData(() -> style.gap, gap -> {
            style.gap = gap;
            element.markTaffyStyleDirty();
        });
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

    public void setFlexBasis(TaffyDimension flexBasis) {
        if (!style.flexBasis.equals(flexBasis)) {
            style.flexBasis = flexBasis;
            element.markTaffyStyleDirty();
        }
    }

    public void setFlex(FloatOptional value) {
        var flex = value.isUndefined() ? Float.NaN : value.getValue();
        if (Float.isNaN(flex) && Float.isNaN(style.flex)) return;
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
//        TaffyPoint<Overflow> overflow = switch (value) {
//            case VISIBLE -> new TaffyPoint<>(Overflow.VISIBLE, Overflow.VISIBLE);
//            case HIDDEN -> new TaffyPoint<>(Overflow.HIDDEN, Overflow.HIDDEN);
//            case SCROLL -> new TaffyPoint<>(Overflow.CLIP, Overflow.CLIP);
//        };
//        if (!style.overflow.equals(overflow)) {
//            style.overflow = overflow;
//            element.markTaffyStyleDirty();
//        }
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
        var aspectRatio = value.isUndefined() ? DEFAULT_TAFFY_STYLE.aspectRatio : value.getValue();
        if (Float.isNaN(style.aspectRatio) && Float.isNaN(aspectRatio)) return;
        if (style.aspectRatio != aspectRatio) {
            style.aspectRatio = aspectRatio;
            element.markTaffyStyleDirty();
        }
    }

    public void setWidth(TaffyDimension width) {
        if (!Objects.equals(style.size.width, width)) {
            style.size = new TaffySize<>(width, style.size.height);
            element.markTaffyStyleDirty();
        }
    }

    public void setHeight(TaffyDimension height) {
        if (!Objects.equals(style.size.height, height)) {
            style.size = new TaffySize<>(style.size.width, height);
            element.markTaffyStyleDirty();
        }
    }

    public void setMinWidth(TaffyDimension minWidth) {
        if (!Objects.equals(style.minSize.width, minWidth)) {
            style.minSize = new TaffySize<>(minWidth, style.minSize.height);
            element.markTaffyStyleDirty();
        }
    }

    public void setMinHeight(TaffyDimension minHeight) {
        if (!Objects.equals(style.minSize.height, minHeight)) {
            style.minSize = new TaffySize<>(style.minSize.width, minHeight);
            element.markTaffyStyleDirty();
        }
    }

    public void setMaxWidth(TaffyDimension maxWidth) {
        if (!Objects.equals(style.maxSize.width, maxWidth)) {
            style.maxSize = new TaffySize<>(maxWidth, style.maxSize.height);
            element.markTaffyStyleDirty();
        }
    }

    public void setMaxHeight(TaffyDimension maxHeight) {
        if (!Objects.equals(style.maxSize.height, maxHeight)) {
            style.maxSize = new TaffySize<>(style.maxSize.width, maxHeight);
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
            element.markTaffyStyleDirty();
        }
    }

    public void setRight(LengthPercentageAuto right) {
        if (!Objects.equals(style.inset.right, right)) {
            style.inset = new TaffyRect<>(style.inset.left, right, style.inset.top, style.inset.bottom);
            element.markTaffyStyleDirty();
        }
    }

    public void setBottom(LengthPercentageAuto bottom) {
        if (!Objects.equals(style.inset.bottom, bottom)) {
            style.inset = new TaffyRect<>(style.inset.left, style.inset.right, style.inset.top, bottom);
            element.markTaffyStyleDirty();
        }
    }

    public static class LPARectData {
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
            if (!Objects.equals(this.rect, rect)) {
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

    public static class LPRectData {
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
            if (!Objects.equals(this.rect, rect)) {
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

    public static class LPSizeData {
        private LengthPercentageAuto vertical = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto horizontal = LengthPercentageAuto.AUTO;
        private LengthPercentageAuto all = LengthPercentageAuto.AUTO;
        private LPSize size = LPSize.ZERO;

        private final Supplier<TaffySize<LengthPercentage>> getter;
        private final Consumer<TaffySize<LengthPercentage>> setter;

        public LPSizeData(Supplier<TaffySize<LengthPercentage>> getter, Consumer<TaffySize<LengthPercentage>> setter) {
            this.setter = setter;
            this.getter = getter;
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

        public void setSize(LPSize size) {
            if (!Objects.equals(this.size, size)) {
                this.size = size;
                onChanged();
            }
        }

        public void onChanged() {
            var current = getter.get();
            var width = (this.horizontal.isAuto() ?
                    (this.all.isAuto() ? this.size.size().width :
                            toLP(this.all)) :
                    toLP(this.horizontal));
            var height = (this.horizontal.isAuto() ?
                    (this.all.isAuto() ? this.size.size().height :
                            toLP(this.all)) :
                    toLP(this.horizontal));
            if (!Objects.equals(width, current.width) ||
                    !Objects.equals(height, current.height)) {
                setter.accept(TaffySize.of(width, height));
            }
        }

        public static LengthPercentage toLP(LengthPercentageAuto lpa) {
            if (lpa.isPercent()) return LengthPercentage.percent(lpa.getValue());
            if (lpa.isLength()) return LengthPercentage.length(lpa.getValue());
            return LengthPercentage.ZERO;
        }
    }
}