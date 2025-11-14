package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.properties.FloatOptionalProperty;
import com.lowdragmc.lowdraglib2.gui.ui.style.properties.StyleLengthProperty;
import com.lowdragmc.lowdraglib2.gui.ui.style.properties.StyleSizeLengthProperty;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.FlexIcons;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.util.TriConsumer;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.List;
import java.util.function.BiConsumer;

@UtilityClass
public final class YogaProperties {
    public static final List<YogaAlign> YOGA_ALIGNS = List.of(
            YogaAlign.AUTO,
            YogaAlign.FLEX_START,
            YogaAlign.CENTER,
            YogaAlign.FLEX_END,
            YogaAlign.STRETCH);
    public static final Property<YogaDisplay> DISPLAY = PropertyRegistry.create("display", YogaDisplay.class, YogaDisplay.FLEX);
    public static final Property<YogaDirection> LAYOUT_DIRECTION = PropertyRegistry.create("layout-direction", YogaDirection.class, YogaDirection.INHERIT);
    public static final Property<StyleSizeLength> FLEX_BASIS = create("flex-basis", StyleSizeLength.AUTO);
    public static final Property<FloatOptional> FLEX = create("flex", FloatOptional.of());
    public static final Property<FloatOptional> FLEX_GROW = create("flex-grow", FloatOptional.of());
    public static final Property<FloatOptional> FLEX_SHRINK = create("flex-shrink", FloatOptional.of());
    public static final Property<YogaFlexDirection> FLEX_DIRECTION = PropertyRegistry.create("flex-direction", YogaFlexDirection.class, YogaFlexDirection.COLUMN).setIconProvider(FlexIcons::getFlexDirectionIcon);
    public static final Property<YogaWrap> FLEX_WRAP = PropertyRegistry.create("flex-wrap", YogaWrap.class, YogaWrap.NO_WRAP).setIconProvider(FlexIcons::getFlexWrapIcon);
    public static final Property<YogaPositionType> POSITION = PropertyRegistry.create("position", YogaPositionType.class, YogaPositionType.RELATIVE);
    public static final Property<StyleLength>[] POSITIONS = createEdge("");
    public static final Property<StyleLength>[] MARGINS = createEdge("margin");
    public static final Property<StyleLength>[] PADDINGS = createEdge("padding");
    public static final Property<StyleLength>[] GAPS = createGutter("gap");
    public static final Property<StyleSizeLength> WIDTH = create("width", StyleSizeLength.ofAuto());
    public static final Property<StyleSizeLength> HEIGHT = create("height", StyleSizeLength.ofAuto());
    public static final Property<StyleSizeLength>[] MIN = createDimension("min");
    public static final Property<StyleSizeLength>[] MAX = createDimension("max");
    public static final Property<FloatOptional> ASPECT_RATE = create("aspect-rate", FloatOptional.of());
    public static final Property<YogaOverflow> OVERFLOW = PropertyRegistry.create("overflow", YogaOverflow.class, YogaOverflow.VISIBLE, List.of(YogaOverflow.VISIBLE, YogaOverflow.HIDDEN));
    public static final Property<YogaAlign> ALIGN_ITEMS = PropertyRegistry.create("align-items", YogaAlign.class, YogaAlign.STRETCH, YOGA_ALIGNS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<YogaJustify> JUSTIFY_CONTENT = PropertyRegistry.create("justify-content", YogaJustify.class, YogaJustify.FLEX_START).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<YogaAlign> ALIGN_SELF = PropertyRegistry.create("align-self", YogaAlign.class, YogaAlign.AUTO, YOGA_ALIGNS).setIconProvider(v -> IGuiTexture.EMPTY);
    public static final Property<YogaAlign> ALIGN_CONTENT = PropertyRegistry.create("align-content", YogaAlign.class, YogaAlign.FLEX_START, YOGA_ALIGNS).setIconProvider(v -> IGuiTexture.EMPTY);

    public static void init() {
        createSetter(YogaProperties.DISPLAY, YogaNode::setDisplay);
        createSetter(YogaProperties.LAYOUT_DIRECTION, YogaNode::setDirection);
        createSetter(YogaProperties.FLEX_BASIS, (node, value) -> {
            var style = node.getStyle();
            if (!style.getFlexBasis().equals(value)) {
                style.setFlexBasis(value);
                node.markDirtyAndPropagate();
            }
        });
        createSetter(YogaProperties.FLEX, (node, value) -> {
            var style = node.getStyle();
            if (!style.getFlex().equals(value)) {
                style.setFlex(value);
                node.markDirtyAndPropagate();
            }
        });
        createSetter(YogaProperties.FLEX_GROW, (node, value) -> {
            var style = node.getStyle();
            if (!style.getFlexGrow().equals(value)) {
                style.setFlexGrow(value);
                node.markDirtyAndPropagate();
            }
        });
        createSetter(YogaProperties.FLEX_SHRINK, (node, value) -> {
            var style = node.getStyle();
            if (!style.getFlexShrink().equals(value)) {
                style.setFlexShrink(value);
                node.markDirtyAndPropagate();
            }
        });
        createSetter(YogaProperties.FLEX_DIRECTION, YogaNode::setFlexDirection);
        createSetter(YogaProperties.FLEX_WRAP, YogaNode::setWrap);
        createSetter(YogaProperties.POSITION, YogaNode::setPositionType);
        createSetter(YogaProperties.OVERFLOW, YogaNode::setOverflow);
        createSetter(YogaProperties.ALIGN_ITEMS, YogaNode::setAlignItems);
        createSetter(YogaProperties.JUSTIFY_CONTENT, YogaNode::setJustifyContent);
        createSetter(YogaProperties.ALIGN_SELF, YogaNode::setAlignSelf);
        createSetter(YogaProperties.ALIGN_CONTENT, YogaNode::setAlignContent);
        createSetter(YogaProperties.ASPECT_RATE, (node, value) -> {
            var style = node.getStyle();
            if (!style.getAspectRatio().equals(value)) {
                style.setAspectRatio(value);
                node.markDirtyAndPropagate();
            }
        });
        createSetter(YogaProperties.WIDTH, (node, value) -> {
            var style = node.getStyle();
            if (!style.getDimension(YogaDimension.WIDTH).equals(value)) {
                style.setDimension(YogaDimension.WIDTH, value);
                node.markDirtyAndPropagate();
            }
        });
        createSetter(YogaProperties.HEIGHT, (node, value) -> {
            var style = node.getStyle();
            if (!style.getDimension(YogaDimension.HEIGHT).equals(value)) {
                style.setDimension(YogaDimension.HEIGHT, value);
                node.markDirtyAndPropagate();
            }
        });
        createEdgeSetter(YogaProperties.POSITIONS, YogaNode::setPosition);
        createEdgeSetter(YogaProperties.MARGINS, YogaNode::setMargin);
        createEdgeSetter(YogaProperties.PADDINGS, YogaNode::setPadding);
        createGutterSetter(YogaProperties.GAPS, YogaNode::setGap);
        createDimensionSetter(YogaProperties.MIN, (node, dim, value) -> {
            var style = node.getStyle();
            if (!style.getMinDimension(dim).equals(value)) {
                style.setMinDimension(dim, value);
                node.markDirtyAndPropagate();
            }
        });
        createDimensionSetter(YogaProperties.MAX, (node, dim, value) -> {
            var style = node.getStyle();
            if (!style.getMaxDimension(dim).equals(value)) {
                style.setMaxDimension(dim, value);
                node.markDirtyAndPropagate();
            }
        });
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

    public static Property<StyleSizeLength>[] createDimension(String name) {
        var handlers = new Property[YogaDimension.values().length];
        for (int i = 0; i < YogaDimension.values().length; i++) {
            handlers[i] = create(name + "-" + YogaDimension.values()[i].toString(), StyleSizeLength.undefined());
        }
        return handlers;
    }

    private static <T> void createSetter(Property<T> property, BiConsumer<YogaNode, T> setter) {
        property.addListener((el, p, oldValue,newValue) ->
                setter.accept(el.getLayoutNode(), newValue == null ? property.initialValue : newValue));
    }

    private static <T> void createEdgeSetter(Property<T>[] properties, TriConsumer<YogaNode, YogaEdge, T> setter) {
        var edges = YogaEdge.values();
        for (int i = 0; i < edges.length; i++) {
            var edge = edges[i];
            createSetter(properties[i], (n, v) -> setter.accept(n, edge, v));
        }
    }

    private static <T> void createGutterSetter(Property<T>[] properties, TriConsumer<YogaNode, YogaGutter, T> setter) {
        var gutters = YogaGutter.values();
        for (int i = 0; i < gutters.length; i++) {
            var gutter = gutters[i];
            createSetter(properties[i], (n, v) -> setter.accept(n, gutter, v));
        }
    }

    private static <T> void createDimensionSetter(Property<T>[] properties, TriConsumer<YogaNode, YogaDimension, T> setter) {
        var dimensions = YogaDimension.values();
        for (int i = 0; i < dimensions.length; i++) {
            var dimension = dimensions[i];
            createSetter(properties[i], (n, v) -> setter.accept(n, dimension, v));
        }
    }
}
