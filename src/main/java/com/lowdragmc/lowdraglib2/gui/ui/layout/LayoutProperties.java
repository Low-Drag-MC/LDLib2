package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.Grid;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridAuto;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplateAreas;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.properties.*;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.FlexIcons;
import dev.vfyjxf.taffy.style.*;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.util.TriConsumer;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

@UtilityClass
public final class LayoutProperties {
    public static final List<AlignItems> DEFAULT_ALIGN_ITEMS = Arrays.asList(
            null,
            AlignItems.START,
            AlignItems.END,
            AlignItems.FLEX_START,
            AlignItems.FLEX_END,
            AlignItems.CENTER,
            AlignItems.STRETCH
    );
    public static final List<AlignContent> DEFAULT_ALIGN_CONTENT = Arrays.asList(
            null,
            AlignContent.START,
            AlignContent.END,
            AlignContent.FLEX_START,
            AlignContent.FLEX_END,
            AlignContent.CENTER,
            AlignContent.SPACE_BETWEEN,
            AlignContent.SPACE_AROUND,
            AlignContent.SPACE_EVENLY,
            AlignContent.STRETCH
    );
    public static final Property<TaffyDisplay> DISPLAY = PropertyRegistry.create("display", TaffyDisplay.class, TaffyDisplay.FLEX);
    public static final Property<TaffyDirection> LAYOUT_DIRECTION = PropertyRegistry.create("layout-direction", TaffyDirection.class, TaffyDirection.INHERIT);
    public static final Property<StyleSizeLength> FLEX_BASIS = create("flex-basis", StyleSizeLength.AUTO);
    public static final Property<FloatOptional> FLEX = create("flex", FloatOptional.of());
    public static final Property<FloatOptional> FLEX_GROW = create("flex-grow", FloatOptional.of());
    public static final Property<FloatOptional> FLEX_SHRINK = create("flex-shrink", FloatOptional.of());
    public static final Property<FlexDirection> FLEX_DIRECTION = PropertyRegistry.create("flex-direction", FlexDirection.class, FlexDirection.COLUMN).setIconProvider(FlexIcons::getFlexDirectionIcon);
    public static final Property<FlexWrap> FLEX_WRAP = PropertyRegistry.create("flex-wrap", FlexWrap.class, FlexWrap.NO_WRAP).setIconProvider(FlexIcons::getFlexWrapIcon);
    public static final Property<TaffyPosition> POSITION = PropertyRegistry.create("position", TaffyPosition.class, TaffyPosition.RELATIVE);
    public static final Property<StyleLength>[] POSITIONS = createEdge("");
    public static final Property<StyleLength>[] MARGINS = createEdge("margin");
    public static final Property<StyleLength>[] PADDINGS = createEdge("padding");
    public static final Property<StyleLength>[] GAPS = createGutter("gap");
    public static final Property<StyleSizeLength> WIDTH = create("width", StyleSizeLength.ofAuto());
    public static final Property<StyleSizeLength> HEIGHT = create("height", StyleSizeLength.ofAuto());
    public static final Property<StyleSizeLength>[] MIN = createDimension("min", StyleSizeLength.points(0));
    public static final Property<StyleSizeLength>[] MAX = createDimension("max", StyleSizeLength.undefined());
    public static final Property<FloatOptional> ASPECT_RATE = create("aspect-rate", FloatOptional.of());
    public static final Property<YogaOverflow> OVERFLOW = PropertyRegistry.create("overflow", YogaOverflow.class, YogaOverflow.VISIBLE, List.of(YogaOverflow.VISIBLE, YogaOverflow.HIDDEN));
    public static final Property<AlignItems> ALIGN_ITEMS = PropertyRegistry.create("align-items", AlignItems.class, null, DEFAULT_ALIGN_ITEMS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignContent> JUSTIFY_CONTENT = PropertyRegistry.create("justify-content", AlignContent.class, null ).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignItems> JUSTIFY_ITEMS = PropertyRegistry.create("justify-items", AlignItems.class, null, DEFAULT_ALIGN_ITEMS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignItems> JUSTIFY_SELF = PropertyRegistry.create("justify-self", AlignItems.class, null, DEFAULT_ALIGN_ITEMS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignItems> ALIGN_SELF = PropertyRegistry.create("align-self", AlignItems.class, null, DEFAULT_ALIGN_ITEMS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<AlignContent> ALIGN_CONTENT = PropertyRegistry.create("align-content", AlignContent.class, AlignContent.FLEX_START, DEFAULT_ALIGN_CONTENT).setIconProvider(v -> IGuiTexture.EMPTY);

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
        createSetter(LayoutProperties.WIDTH, TaffyLayoutStyle::setWidth);
        createSetter(LayoutProperties.HEIGHT, TaffyLayoutStyle::setHeight);
        createEdgeSetter(LayoutProperties.POSITIONS, TaffyLayoutStyle::setInset);
        createEdgeSetter(LayoutProperties.MARGINS, TaffyLayoutStyle::setMargin);
        createEdgeSetter(LayoutProperties.PADDINGS, TaffyLayoutStyle::setPadding);
        createGutterSetter(LayoutProperties.GAPS, TaffyLayoutStyle::setGap);
        createDimensionSetter(LayoutProperties.MIN,TaffyLayoutStyle::setMinSize);
        createDimensionSetter(LayoutProperties.MAX, TaffyLayoutStyle::setMaxSize);

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

    public static Property<StyleSizeLength> create(String name, StyleSizeLength initialValue) {
        return PropertyRegistry.create(new StyleSizeLengthProperty(name, initialValue));
    }

    public static Property<StyleLength> create(String name, StyleLength initialValue) {
        return PropertyRegistry.create(new StyleLengthProperty(name, initialValue));
    }

    public static Property<FloatOptional> create(String name, FloatOptional initialValue) {
        return PropertyRegistry.create(new FloatOptionalProperty(name, initialValue));
    }

    public static Property<StyleLength>[] createEdge(String name) {
        var handlers = new Property[YogaEdge.values().length];
        for (int i = 0; i < YogaEdge.values().length; i++) {
            handlers[i] = create((name.isEmpty() ? "" : (name + "-")) + YogaEdge.values()[i].toString(), StyleLength.undefined())
                    .setConfigName(YogaEdge.values()[i].toString());
        }
        return handlers;
    }

    public static Property<StyleLength>[] createGutter(String name) {
        var handlers = new Property[YogaGutter.values().length];
        for (int i = 0; i < YogaGutter.values().length; i++) {
            handlers[i] = create(name + "-" + YogaGutter.values()[i].toString(), StyleLength.undefined())
                    .setConfigName(YogaGutter.values()[i].toString());
        }
        return handlers;
    }

    public static Property<StyleSizeLength>[] createDimension(String name, StyleSizeLength defaultValue) {
        var handlers = new Property[YogaDimension.values().length];
        for (int i = 0; i < YogaDimension.values().length; i++) {
            handlers[i] = create(name + "-" + YogaDimension.values()[i].toString(), defaultValue);
        }
        return handlers;
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

    private static <T> void createEdgeSetter(Property<T>[] properties,
                                             TriConsumer<TaffyLayoutStyle, YogaEdge, T> taffySetter) {
        var edges = YogaEdge.values();
        for (int i = 0; i < edges.length; i++) {
            var edge = edges[i];
            createSetter(properties[i],
                    (s, v) -> taffySetter.accept(s, edge, v));
        }
    }

    private static <T> void createGutterSetter(Property<T>[] properties,
                                               TriConsumer<TaffyLayoutStyle, YogaGutter, T> taffySetter) {
        var gutters = YogaGutter.values();
        for (int i = 0; i < gutters.length; i++) {
            var gutter = gutters[i];
            createSetter(properties[i],
                    (s, v) -> taffySetter.accept(s, gutter, v));
        }
    }

    private static <T> void createDimensionSetter(Property<T>[] properties,
                                                  TriConsumer<TaffyLayoutStyle, YogaDimension, T> taffySetter) {
        var dimensions = YogaDimension.values();
        for (int i = 0; i < dimensions.length; i++) {
            var dimension = dimensions[i];
            createSetter(properties[i],
                    (s, v) -> taffySetter.accept(s, dimension, v));
        }
    }
}
