package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.properties.*;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.FlexIcons;
import dev.vfyjxf.taffy.style.*;
import lombok.experimental.UtilityClass;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

@UtilityClass
public final class LayoutProperties {
    public static final List<AlignItems> DEFAULT_ALIGN_ITEMS = Arrays.asList(
            AlignItems.AUTO,
            AlignItems.START,
            AlignItems.END,
            AlignItems.FLEX_START,
            AlignItems.FLEX_END,
            AlignItems.CENTER,
            AlignItems.STRETCH
    );

    public static final Property<TaffyDisplay> DISPLAY = PropertyRegistry.create("display", TaffyDisplay.class, TaffyDisplay.FLEX);
    public static final Property<TaffyDirection> LAYOUT_DIRECTION = PropertyRegistry.create("layout-direction", TaffyDirection.class, TaffyDirection.INHERIT);
    public static final Property<TaffyDimension> FLEX_BASIS = create("flex-basis", TaffyDimension.auto());
    public static final Property<FloatOptional> FLEX = create("flex", FloatOptional.of());
    public static final Property<FloatOptional> FLEX_GROW = create("flex-grow", FloatOptional.of());
    public static final Property<FloatOptional> FLEX_SHRINK = create("flex-shrink", FloatOptional.of(0));
    public static final Property<FlexDirection> FLEX_DIRECTION = PropertyRegistry.create("flex-direction", FlexDirection.class, FlexDirection.COLUMN).setIconProvider(FlexIcons::getFlexDirectionIcon);
    public static final Property<FlexWrap> FLEX_WRAP = PropertyRegistry.create("flex-wrap", FlexWrap.class, FlexWrap.NO_WRAP).setIconProvider(FlexIcons::getFlexWrapIcon);
    public static final Property<TaffyPosition> POSITION = PropertyRegistry.create("position", TaffyPosition.class, TaffyPosition.RELATIVE);

    public static final Property<LengthPercentageAuto> LEFT = create("left", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> TOP = create("top", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> RIGHT = create("right", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> BOTTOM = create("bottom", LengthPercentageAuto.AUTO);

    public static final Property<LengthPercentageAuto> MARGIN_LEFT = create("margin-left", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> MARGIN_TOP = create("margin-top", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> MARGIN_RIGHT = create("margin-right", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> MARGIN_BOTTOM = create("margin-bottom", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> MARGIN_VERTICAL = create("margin-vertical", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> MARGIN_HORIZONTAL = create("margin-horizontal", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> MARGIN_ALL = create("margin-all", LengthPercentageAuto.AUTO);
    public static final Property<LPARect> MARGIN = create("margin", LPARect.ZERO);

    public static final Property<LengthPercentageAuto> PADDING_LEFT = create("padding-left", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> PADDING_TOP = create("padding-top", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> PADDING_RIGHT = create("padding-right", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> PADDING_BOTTOM = create("padding-bottom", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> PADDING_VERTICAL = create("padding-vertical", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> PADDING_HORIZONTAL = create("padding-horizontal", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> PADDING_ALL = create("padding-all", LengthPercentageAuto.AUTO);
    public static final Property<LPARect> PADDING = create("padding", LPARect.ZERO);

    public static final Property<LengthPercentageAuto> GAP_ROW = create("gap-row", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> GAP_COLUMN = create("gap-column", LengthPercentageAuto.AUTO);
    public static final Property<LengthPercentageAuto> GAP_ALL = create("gap-all", LengthPercentageAuto.AUTO);
    public static final Property<LPSize> GAP = create("gap", LPSize.ZERO);

    public static final Property<TaffyDimension> WIDTH = create("width", TaffyDimension.auto());
    public static final Property<TaffyDimension> HEIGHT = create("height", TaffyDimension.auto());

    public static final Property<TaffyDimension> MIN_WIDTH = create("min-width", TaffyDimension.ZERO);
    public static final Property<TaffyDimension> MIN_HEIGHT = create("min-height", TaffyDimension.ZERO);

    public static final Property<TaffyDimension> MAX_WIDTH = create("max-width", TaffyDimension.auto());
    public static final Property<TaffyDimension> MAX_HEIGHT = create("max-height", TaffyDimension.auto());

    public static final Property<FloatOptional> ASPECT_RATE = create("aspect-rate", FloatOptional.of());
    // TODO overflow and clip?
    public static final Property<YogaOverflow> OVERFLOW = PropertyRegistry.create("overflow", YogaOverflow.class, YogaOverflow.VISIBLE, List.of(YogaOverflow.VISIBLE, YogaOverflow.HIDDEN));
    public static final Property<AlignItems> ALIGN_ITEMS = PropertyRegistry.create("align-items", AlignItems.class, AlignItems.STRETCH, DEFAULT_ALIGN_ITEMS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignItems> ALIGN_SELF = PropertyRegistry.create("align-self", AlignItems.class, AlignItems.AUTO, DEFAULT_ALIGN_ITEMS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignContent> ALIGN_CONTENT = PropertyRegistry.create("align-content", AlignContent.class, AlignContent.FLEX_START).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignItems> JUSTIFY_ITEMS = PropertyRegistry.create("justify-items", AlignItems.class, AlignItems.AUTO, DEFAULT_ALIGN_ITEMS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignItems> JUSTIFY_SELF = PropertyRegistry.create("justify-self", AlignItems.class, AlignItems.AUTO, DEFAULT_ALIGN_ITEMS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignContent> JUSTIFY_CONTENT = PropertyRegistry.create("justify-content", AlignContent.class, AlignContent.FLEX_START).setIconProvider(v -> IGuiTexture.EMPTY);

    public static final Property<GridTemplate> GRID_TEMPLATE_ROWS = create("grid-template-rows", GridTemplate.EMPTY);
    public static final Property<GridTemplate> GRID_TEMPLATE_COLUMNS = create("grid-template-columns", GridTemplate.EMPTY);
    public static final Property<GridTemplateAreas> GRID_TEMPLATE_AREAS = create("grid-template-areas", GridTemplateAreas.EMPTY);
    public static final Property<GridAuto> GRID_AUTO_ROWS = create("grid-auto-rows", GridAuto.EMPTY);
    public static final Property<GridAuto> GRID_AUTO_COLUMNS = create("grid-auto-columns", GridAuto.EMPTY);
    public static final Property<GridAutoFlow> GRID_AUTO_FLOW = PropertyRegistry.create("grid-auto-flow", GridAutoFlow.class, GridAutoFlow.ROW);
    public static final Property<Grid> GRID_ROW = create("grid-row", Grid.EMPTY);
    public static final Property<Grid> GRID_COLUMN = create("grid-column", Grid.EMPTY);

    public static void init() {
        createSetter(LayoutProperties.DISPLAY, TaffyLayoutStyle::setDisplay);
        createSetter(LayoutProperties.LAYOUT_DIRECTION, TaffyLayoutStyle::setDirection);
        createSetter(LayoutProperties.FLEX_BASIS, TaffyLayoutStyle::setFlexBasis);
        createSetter(LayoutProperties.FLEX, TaffyLayoutStyle::setFlex);
        createSetter(LayoutProperties.FLEX_GROW, TaffyLayoutStyle::setFlexGrow);
        createSetter(LayoutProperties.FLEX_SHRINK, TaffyLayoutStyle::setFlexShrink);
        createSetter(LayoutProperties.FLEX_DIRECTION, TaffyLayoutStyle::setFlexDirection);
        createSetter(LayoutProperties.FLEX_WRAP, TaffyLayoutStyle::setFlexWrap);
        createSetter(LayoutProperties.POSITION, TaffyLayoutStyle::setPosition);
        createSetter(LayoutProperties.OVERFLOW, TaffyLayoutStyle::setOverFlow);
        createSetter(LayoutProperties.ALIGN_ITEMS, TaffyLayoutStyle::setAlignItems);
        createSetter(LayoutProperties.JUSTIFY_CONTENT, TaffyLayoutStyle::setJustifyContent);
        createSetter(LayoutProperties.JUSTIFY_ITEMS, TaffyLayoutStyle::setJustifyItems);
        createSetter(LayoutProperties.JUSTIFY_SELF, TaffyLayoutStyle::setJustifySelf);
        createSetter(LayoutProperties.ALIGN_SELF, TaffyLayoutStyle::setAlignSelf);
        createSetter(LayoutProperties.ALIGN_CONTENT, TaffyLayoutStyle::setAlignContent);
        createSetter(LayoutProperties.ASPECT_RATE, TaffyLayoutStyle::setAspectRate);

        createSetter(LayoutProperties.LEFT, TaffyLayoutStyle::setLeft);
        createSetter(LayoutProperties.TOP, TaffyLayoutStyle::setTop);
        createSetter(LayoutProperties.RIGHT, TaffyLayoutStyle::setRight);
        createSetter(LayoutProperties.BOTTOM, TaffyLayoutStyle::setBottom);

        createSetter(LayoutProperties.WIDTH, TaffyLayoutStyle::setWidth);
        createSetter(LayoutProperties.HEIGHT, TaffyLayoutStyle::setHeight);
        createSetter(LayoutProperties.MIN_WIDTH, TaffyLayoutStyle::setMinWidth);
        createSetter(LayoutProperties.MAX_WIDTH, TaffyLayoutStyle::setMaxWidth);
        createSetter(LayoutProperties.MIN_HEIGHT, TaffyLayoutStyle::setMinHeight);
        createSetter(LayoutProperties.MAX_HEIGHT, TaffyLayoutStyle::setMaxHeight);

        createSetter(LayoutProperties.MARGIN_LEFT, (style, value) -> style.margin.setLeft(value));
        createSetter(LayoutProperties.MARGIN_TOP, (style, value) -> style.margin.setTop(value));
        createSetter(LayoutProperties.MARGIN_RIGHT, (style, value) -> style.margin.setRight(value));
        createSetter(LayoutProperties.MARGIN_BOTTOM, (style, value) -> style.margin.setBottom(value));
        createSetter(LayoutProperties.MARGIN_VERTICAL, (style, value) -> style.margin.setVertical(value));
        createSetter(LayoutProperties.MARGIN_HORIZONTAL, (style, value) -> style.margin.setHorizontal(value));
        createSetter(LayoutProperties.MARGIN_ALL, (style, value) -> style.margin.setAll(value));
        createSetter(LayoutProperties.MARGIN, (style, value) -> style.margin.setRect(value));

        createSetter(LayoutProperties.PADDING_LEFT, (style, value) -> style.padding.setLeft(value));
        createSetter(LayoutProperties.PADDING_TOP, (style, value) -> style.padding.setTop(value));
        createSetter(LayoutProperties.PADDING_RIGHT, (style, value) -> style.padding.setRight(value));
        createSetter(LayoutProperties.PADDING_BOTTOM, (style, value) -> style.padding.setBottom(value));
        createSetter(LayoutProperties.PADDING_VERTICAL, (style, value) -> style.padding.setVertical(value));
        createSetter(LayoutProperties.PADDING_HORIZONTAL, (style, value) -> style.padding.setHorizontal(value));
        createSetter(LayoutProperties.PADDING_ALL, (style, value) -> style.padding.setAll(value));
        createSetter(LayoutProperties.PADDING, (style, value) -> style.padding.setRect(value));

        createSetter(LayoutProperties.GAP_ROW, (style, value) -> style.gap.setVertical(value));
        createSetter(LayoutProperties.GAP_COLUMN, (style, value) -> style.gap.setHorizontal(value));
        createSetter(LayoutProperties.GAP_ALL, (style, value) -> style.gap.setAll(value));
        createSetter(LayoutProperties.GAP, (style, value) -> style.gap.setSize(value));

        // Grid properties (Taffy-specific, no Yoga equivalents)
        createSetter(LayoutProperties.GRID_TEMPLATE_ROWS, TaffyLayoutStyle::setGridTemplateRows);
        createSetter(LayoutProperties.GRID_TEMPLATE_COLUMNS, TaffyLayoutStyle::setGridTemplateColumns);
        createSetter(LayoutProperties.GRID_TEMPLATE_AREAS, TaffyLayoutStyle::setGridTemplateAreas);
        createSetter(LayoutProperties.GRID_AUTO_ROWS, TaffyLayoutStyle::setGridAutoRows);
        createSetter(LayoutProperties.GRID_AUTO_COLUMNS, TaffyLayoutStyle::setGridAutoColumns);
        createSetter(LayoutProperties.GRID_AUTO_FLOW, TaffyLayoutStyle::setGridAutoFlow);
        createSetter(LayoutProperties.GRID_ROW, TaffyLayoutStyle::setGridRow);
        createSetter(LayoutProperties.GRID_COLUMN, TaffyLayoutStyle::setGridColumn);
    }

    public static Property<FloatOptional> create(String name, FloatOptional initialValue) {
        return PropertyRegistry.create(new FloatOptionalProperty(name, initialValue));
    }

    public static Property<LengthPercentageAuto> create(String name, LengthPercentageAuto initialValue) {
        return PropertyRegistry.create(new LPAProperty(name, initialValue));
    }

    public static Property<LPARect> create(String name, LPARect initialValue) {
        return PropertyRegistry.create(new LPARectProperty(name, initialValue));
    }

    public static Property<LPSize> create(String name, LPSize initialValue) {
        return PropertyRegistry.create(new LPSizeProperty(name, initialValue));
    }

    public static Property<TaffyDimension> create(String name, TaffyDimension initialValue) {
        return PropertyRegistry.create(new DimensionProperty(name, initialValue));
    }

    public static Property<GridTemplate> create(String name, GridTemplate initialValue) {
        return PropertyRegistry.create(new GridTemplateProperty(name, initialValue));
    }

    public static Property<GridAuto> create(String name, GridAuto initialValue) {
        return PropertyRegistry.create(new GridAutoProperty(name, initialValue));
    }

    public static Property<GridTemplateAreas> create(String name, GridTemplateAreas initialValue) {
        return PropertyRegistry.create(new GridTemplateAreasProperty(name, initialValue));
    }

    public static Property<Grid> create(String name, Grid initialValue) {
        return PropertyRegistry.create(new GridProperty(name, initialValue));
    }

    private static <T> void createSetter(Property<T> property,
                                         BiConsumer<TaffyLayoutStyle, T> taffySetter) {
        property.addListener((el, p, oldValue, newValue) ->
                taffySetter.accept(el.getTaffyStyle(), newValue == null ? property.initialValue : newValue));
    }
}
