package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleHandler;
import com.lowdragmc.lowdraglib2.gui.ui.style.UIStyleRegistries;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.*;
import lombok.experimental.UtilityClass;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.Map;
import java.util.function.BiConsumer;

@UtilityClass
public final class YogaNodeStyleParser {
    public static final StyleHandler<YogaDisplay> DISPLAY = UIStyleRegistries.create("display", EnumValue.of(YogaDisplay.class));
    public static final StyleHandler<YogaDirection> LAYOUT_DIRECTION = UIStyleRegistries.create("layout-direction", EnumValue.of(YogaDirection.class));
    public static final StyleHandler<StyleSizeLength> FLEX_BASIS = UIStyleRegistries.create("flex-basis", StyleSizeLengthValue::new);
    public static final StyleHandler<FloatOptional> FLEX = UIStyleRegistries.create("flex", FloatOptionalValue::new);
    public static final StyleHandler<FloatOptional> FLEX_GROW = UIStyleRegistries.create("flex-grow", FloatOptionalValue::new);
    public static final StyleHandler<FloatOptional> FLEX_SHRINK = UIStyleRegistries.create("flex-shrink", FloatOptionalValue::new);
    public static final StyleHandler<YogaFlexDirection> FLEX_DIRECTION = UIStyleRegistries.create("flex-direction", EnumValue.of(YogaFlexDirection.class));
    public static final StyleHandler<YogaWrap> FLEX_WRAP = UIStyleRegistries.create("flex-wrap", EnumValue.of(YogaWrap.class));
    public static final StyleHandler<YogaPositionType> POSITION = UIStyleRegistries.create("position", EnumValue.of(YogaPositionType.class));
    public static final StyleHandler<StyleLength>[] POSITIONS = createEdge("position");
    public static final StyleHandler<StyleLength>[] MARGINS = createEdge("margin");
    public static final StyleHandler<StyleLength>[] PADDINGS = createEdge("padding");
    public static final StyleHandler<StyleLength>[] GAPS = createGutter("gap");
    public static final StyleHandler<StyleSizeLength> WIDTH = UIStyleRegistries.create("width", StyleSizeLengthValue::new);
    public static final StyleHandler<StyleSizeLength> HEIGHT = UIStyleRegistries.create("height", StyleSizeLengthValue::new);
    public static final StyleHandler<StyleSizeLength>[] MIN = createDimension("min");
    public static final StyleHandler<StyleSizeLength>[] MAX = createDimension("max");
    public static final StyleHandler<FloatOptional> ASPECT_RATE = UIStyleRegistries.create("aspect-rate", FloatOptionalValue::new);
    public static final StyleHandler<YogaOverflow> OVERFLOW = UIStyleRegistries.create("overflow", EnumValue.of(YogaOverflow.class));
    public static final StyleHandler<YogaAlign> ALIGN_ITEMS = UIStyleRegistries.create("align-items", EnumValue.of(YogaAlign.class));
    public static final StyleHandler<YogaJustify> JUSTIFY_CONTENT = UIStyleRegistries.create("justify-content", EnumValue.of(YogaJustify.class));
    public static final StyleHandler<YogaAlign> ALIGN_SELF = UIStyleRegistries.create("align-self", EnumValue.of(YogaAlign.class));
    public static final StyleHandler<YogaAlign> ALIGN_CONTENT = UIStyleRegistries.create("align-content", EnumValue.of(YogaAlign.class));

    public static void init() {}

    public static void applyStyles(UIElement uiElement, Map<String, StyleValue<?>> values) {
        var yogaNode = uiElement.getLayoutNode();
        var yogaStyle = yogaNode.getStyle();

        DISPLAY.parse(values).ifPresent(yogaNode::setDisplay);
        LAYOUT_DIRECTION.parse(values).ifPresent(yogaStyle::setDirection);
        FLEX_BASIS.parse(values).ifPresent(value -> {
            yogaStyle.setFlexBasis(value);
            yogaNode.markDirtyAndPropagate();
        });
        FLEX.parse(values).ifPresent(value -> {
            yogaStyle.setFlex(value);
            yogaNode.markDirtyAndPropagate();
        });
        FLEX_GROW.parse(values).ifPresent(value -> {
            yogaStyle.setFlexGrow(value);
            yogaNode.markDirtyAndPropagate();
        });
        FLEX_SHRINK.parse(values).ifPresent(value -> {
            yogaStyle.setFlexShrink(value);
            yogaNode.markDirtyAndPropagate();
        });
        FLEX_DIRECTION.parse(values).ifPresent(yogaNode::setFlexDirection);
        FLEX_WRAP.parse(values).ifPresent(yogaNode::setWrap);
        POSITION.parse(values).ifPresent(yogaStyle::setPositionType);
        parseEdge(POSITIONS, values, yogaNode::setPosition);
        parseEdge(MARGINS, values, yogaNode::setMargin);
        parseEdge(PADDINGS, values, yogaNode::setPadding);
        parseGutter(GAPS, values, yogaNode::setGap);
        WIDTH.parse(values).ifPresent(value -> {
            yogaStyle.setDimension(YogaDimension.WIDTH, value);
            yogaNode.markDirtyAndPropagate();
        });
        HEIGHT.parse(values).ifPresent(value -> {
            yogaStyle.setDimension(YogaDimension.HEIGHT, value);
            yogaNode.markDirtyAndPropagate();
        });
        parseDimension(MIN, values, (dim, value) -> {
            yogaStyle.setMinDimension(dim, value);
            yogaNode.markDirtyAndPropagate();
        });
        parseDimension(MAX, values, (dim, value) -> {
            yogaStyle.setMaxDimension(dim, value);
            yogaNode.markDirtyAndPropagate();
        });
        ASPECT_RATE.parse(values).ifPresent(value -> {
            yogaStyle.setAspectRatio(value);
            yogaNode.markDirtyAndPropagate();
        });
        OVERFLOW.parse(values).ifPresent(yogaNode::setOverflow);
        ALIGN_ITEMS.parse(values).ifPresent(yogaNode::setAlignItems);
        JUSTIFY_CONTENT.parse(values).ifPresent(yogaNode::setJustifyContent);
        ALIGN_SELF.parse(values).ifPresent(yogaNode::setAlignSelf);
        ALIGN_CONTENT.parse(values).ifPresent(yogaNode::setAlignContent);
    }

    public static StyleHandler<StyleLength>[] createEdge(String name) {
        var handlers = new StyleHandler[YogaEdge.values().length];
        for (int i = 0; i < YogaEdge.values().length; i++) {
            handlers[i] = UIStyleRegistries.create(name + "-" + YogaEdge.values()[i].toString(), StyleLengthValue::new);
        }
        return handlers;
    }

    public static void parseEdge(StyleHandler<StyleLength>[] handlers, Map<String, StyleValue<?>> values,
                                 BiConsumer<YogaEdge, StyleLength> setter) {
        var edges = YogaEdge.values();
        for (int i = 0; i < edges.length; i++) {
            var edge = edges[i];
            handlers[i].parse(values).ifPresent(value -> setter.accept(edge, value));
        }
    }

    public static StyleHandler<StyleLength>[] createGutter(String name) {
        var handlers = new StyleHandler[YogaGutter.values().length];
        for (int i = 0; i < YogaGutter.values().length; i++) {
            handlers[i] = UIStyleRegistries.create(name + "-" + YogaEdge.values()[i].toString(), StyleLengthValue::new);
        }
        return handlers;
    }

    public static void parseGutter(StyleHandler<StyleLength>[] handlers, Map<String, StyleValue<?>> values,
                                 BiConsumer<YogaGutter, StyleLength> setter) {
        var gutters = YogaGutter.values();
        for (int i = 0; i < gutters.length; i++) {
            var gutter = gutters[i];
            handlers[i].parse(values).ifPresent(value -> setter.accept(gutter, value));
        }
    }

    public static StyleHandler<StyleSizeLength>[] createDimension(String name) {
        var handlers = new StyleHandler[YogaDimension.values().length];
        for (int i = 0; i < YogaDimension.values().length; i++) {
            handlers[i] = UIStyleRegistries.create(name + "-" + YogaDimension.values()[i].toString(), StyleLengthValue::new);
        }
        return handlers;
    }

    public static void parseDimension(StyleHandler<StyleSizeLength>[] handlers, Map<String, StyleValue<?>> values,
                                 BiConsumer<YogaDimension, StyleSizeLength> setter) {
        var dimensions = YogaDimension.values();
        for (int i = 0; i < dimensions.length; i++) {
            var dimension = dimensions[i];
            handlers[i].parse(values).ifPresent(value -> setter.accept(dimension, value));
        }
    }
}
